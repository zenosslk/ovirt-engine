package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.BackendService;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.RunVmParams;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmPool;
import org.ovirt.engine.core.common.businessentities.VmPoolMap;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.ErrorMessageUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.timer.SchedulerUtilQuartzImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class VmPoolMonitor implements BackendService {

    private static final Logger log = LoggerFactory.getLogger(VmPoolMonitor.class);

    private String poolMonitoringJobId;
    @Inject
    private SchedulerUtilQuartzImpl schedulerUtil;
    @PostConstruct
    private void init() {
        int vmPoolMonitorIntervalInMinutes = Config.<Integer>getValue(ConfigValues.VmPoolMonitorIntervalInMinutes);
        poolMonitoringJobId =
                schedulerUtil.scheduleAFixedDelayJob(
                        this,
                        "managePrestartedVmsInAllVmPools",
                        new Class[] {},
                        new Object[] {},
                        vmPoolMonitorIntervalInMinutes,
                        vmPoolMonitorIntervalInMinutes,
                        TimeUnit.MINUTES);
    }

    /**
     * Goes over each Vmpool, and makes sure there are at least as much prestarted Vms as defined in the prestarted_vms
     * field
     */
    @OnTimerMethodAnnotation("managePrestartedVmsInAllVmPools")
    public void managePrestartedVmsInAllVmPools() {
        List<VmPool> vmPools = DbFacade.getInstance().getVmPoolDao().getAll();
        for (VmPool vmPool : vmPools) {
            managePrestartedVmsInPool(vmPool);
        }
    }

    public void triggerPoolMonitoringJob() {
        schedulerUtil.triggerJob(poolMonitoringJobId);
    }

    /**
     * Checks how many prestarted vms are missing in the pool, and attempts to prestart either that amount or BATCH_SIZE
     * (the minimum between the two).
     */
    private void managePrestartedVmsInPool(VmPool vmPool) {
        Guid vmPoolId = vmPool.getVmPoolId();
        int prestartedVms = VmPoolCommandBase.getNumOfPrestartedVmsInPool(vmPoolId, new ArrayList<>());
        int missingPrestartedVms = vmPool.getPrestartedVms() - prestartedVms;
        if (missingPrestartedVms > 0) {
            // We do not want to start too many vms at once
            int numOfVmsToPrestart =
                    Math.min(missingPrestartedVms, Config.<Integer> getValue(ConfigValues.VmPoolMonitorBatchSize));

            log.info("VmPool '{}' is missing {} prestarted VMs, attempting to prestart {} VMs",
                    vmPoolId,
                    missingPrestartedVms,
                    numOfVmsToPrestart);
            prestartVms(vmPoolId, numOfVmsToPrestart);
        }
    }

    /***
     * Prestarts the given amount of vmsToPrestart, in the given Vm Pool
     */
    private void prestartVms(Guid vmPoolId, int numOfVmsToPrestart) {
        // Fetch all vms that are in status down
        List<VmPoolMap> vmPoolMaps = DbFacade.getInstance().getVmPoolDao()
                .getVmMapsInVmPoolByVmPoolIdAndStatus(vmPoolId, VMStatus.Down);
        int failedAttempts = 0;
        int prestartedVmsCounter = 0;
        final int maxFailedAttempts = Config.<Integer> getValue(ConfigValues.VmPoolMonitorMaxAttempts);
        Map<String, Integer> failureReasons = new HashMap<>();
        if (vmPoolMaps != null && vmPoolMaps.size() > 0) {
            for (VmPoolMap map : vmPoolMaps) {
                if (failedAttempts < maxFailedAttempts && prestartedVmsCounter < numOfVmsToPrestart) {
                    List<String> messages = new ArrayList<>();
                    if (prestartVm(map.getVmId(), messages)) {
                        prestartedVmsCounter++;
                        failedAttempts = 0;
                    } else {
                        failedAttempts++;
                        collectVmPrestartFailureReasons(failureReasons, messages);
                    }
                } else {
                    // If we reached the required amount or we exceeded the number of allowed failures, stop
                    logResultOfPrestartVms(prestartedVmsCounter, numOfVmsToPrestart, vmPoolId, failureReasons);
                    break;
                }
            }
        } else {
            log.info("No VMs available for prestarting");
        }
    }

    private void collectVmPrestartFailureReasons(Map<String, Integer> failureReasons, List<String> messages) {
        if (log.isInfoEnabled()) {
            String reason = messages.stream()
                    .filter(ErrorMessageUtils::isMessage)
                    .collect(Collectors.joining(", "));
            Integer count = failureReasons.get(reason);
            failureReasons.put(reason, count == null ? 1 : count + 1);
        }
    }

    /**
     * Logs the results of the attempt to prestart Vms in a Vm Pool
     */
    private void logResultOfPrestartVms(int prestartedVmsCounter,
            int numOfVmsToPrestart,
            Guid vmPoolId,
            Map<String, Integer> failureReasons) {
        if (prestartedVmsCounter > 0) {
            log.info("Prestarted {} VMs out of the {} required, in VmPool '{}'",
                    prestartedVmsCounter,
                    numOfVmsToPrestart,
                    vmPoolId);
        } else {
            log.info("Failed to prestart any VMs for VmPool '{}'",
                    vmPoolId);
        }

        if (prestartedVmsCounter < numOfVmsToPrestart) {
            for (Map.Entry<String, Integer> entry : failureReasons.entrySet()) {
                log.info("Failed to prestart {} VMs with reason {}",
                        entry.getValue(),
                        entry.getKey());
            }
        }
    }

    /**
     * Prestarts the given Vm
     * @return whether or not succeeded to prestart the Vm
     */
    private boolean prestartVm(Guid vmGuid, List<String> messages) {
        if (VmPoolCommandBase.canAttachNonPrestartedVmToUser(vmGuid, messages)) {
            VM vmToPrestart = DbFacade.getInstance().getVmDao().get(vmGuid);
            return runVmAsStateless(vmToPrestart);
        }
        return false;
    }

    /**
     * Run the given VM as stateless
     */
    private boolean runVmAsStateless(VM vmToRunAsStateless) {
        log.info("Running VM '{}' as stateless", vmToRunAsStateless);
        RunVmParams runVmParams = new RunVmParams(vmToRunAsStateless.getId());
        runVmParams.setEntityInfo(new EntityInfo(VdcObjectType.VM, vmToRunAsStateless.getId()));
        runVmParams.setRunAsStateless(true);
        VdcReturnValueBase vdcReturnValue = Backend.getInstance().runInternalAction(VdcActionType.RunVm,
                runVmParams, ExecutionHandler.createInternalJobContext());
        boolean prestartingVmSucceeded = vdcReturnValue.getSucceeded();

        if (!prestartingVmSucceeded) {
            AuditLogableBase log = new AuditLogableBase();
            log.addCustomValue("VmPoolName", vmToRunAsStateless.getVmPoolName());
            new AuditLogDirector().log(log, AuditLogType.VM_FAILED_TO_PRESTART_IN_POOL);
        }

        log.info("Running VM '{}' as stateless {}",
                vmToRunAsStateless, prestartingVmSucceeded ? "succeeded" : "failed");
        return prestartingVmSucceeded;
    }

}
