package org.ovirt.engine.core.bll;

import static org.ovirt.engine.core.utils.MacAddressRangeUtils.filterOverlappingRanges;
import static org.ovirt.engine.core.utils.MacAddressRangeUtils.poolsOverlappedByOtherPool;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.ovirt.engine.core.bll.network.macpool.MacPoolPerCluster;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.MacPool;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.dao.ClusterDao;
import org.ovirt.engine.core.dao.MacPoolDao;
import org.ovirt.engine.core.di.Injector;
import org.ovirt.engine.core.utils.ReplacementUtils;

public class MacPoolValidator {

    private final MacPool macPool;

    public MacPoolValidator(MacPool macPool) {
        this.macPool = macPool;
    }

    public ValidationResult notRemovingDefaultPool() {
        return ValidationResult.failWith(EngineMessage.ACTION_TYPE_FAILED_CANNOT_REMOVE_DEFAULT_MAC_POOL).
                when(macPool.isDefaultPool());
    }

    public ValidationResult notRemovingUsedPool() {
        final ClusterDao clusterDao = Injector.get(ClusterDao.class);
        final List<Cluster> clusters = clusterDao.getAllClustersByMacPoolId(macPool.getId());

        final Collection<String> replacements = ReplacementUtils.replaceWithNameable("CLUSTERS_USING_MAC_POOL", clusters);
        replacements.add(EngineMessage.VAR__ENTITIES__CLUSTERS.name());
        return ValidationResult.failWith(EngineMessage.ACTION_TYPE_FAILED_CANNOT_REMOVE_STILL_USED_MAC_POOL,
                replacements.toArray(new String[replacements.size()])).when(clusters.size() != 0);
    }

    public ValidationResult macPoolExists() {
        return ValidationResult.failWith(EngineMessage.ACTION_TYPE_FAILED_MAC_POOL_DOES_NOT_EXIST).
                when(macPool == null);
    }

    public ValidationResult defaultPoolFlagIsNotSet() {
        return ValidationResult.failWith(EngineMessage.ACTION_TYPE_FAILED_SETTING_DEFAULT_MAC_POOL_IS_NOT_SUPPORTED).
                when(macPool.isDefaultPool());
    }

    public ValidationResult hasUniqueName() {
        return ValidationResult.failWith(EngineMessage.ACTION_TYPE_FAILED_NAME_ALREADY_USED).
                        when(!macPoolNameUnique());
    }

    private boolean macPoolNameUnique() {
        final List<MacPool> macPools = getMacPoolDao().getAll();
        for (MacPool pool : macPools) {
            if (!Objects.equals(pool.getId(), macPool.getId()) &&
                    pool.getName().equals(macPool.getName())) {
                return false;
            }
        }

        return true;
    }

    private MacPoolDao getMacPoolDao() {
        return Injector.get(MacPoolDao.class);
    }

    /**
     * Test that if the 'allow duplicates' flag of the mac pool has been
     * turned off there are no duplicates in the pool. For backward compatibility
     * check that the flag has been actually modified so that the validation does
     * not fail on old mac pools where the flag is unset but duplicates exist
     */
    public ValidationResult validateDuplicates(MacPoolPerCluster macPoolPerCluster) {
        return ValidationResult.failWith(EngineMessage.ACTION_TYPE_FAILED_MAC_POOL_CONTAINS_DUPLICATES)
            .when(!macPool.isAllowDuplicateMacAddresses() &&
                macPoolPerCluster.isDuplicateMacAddressesAllowed(macPool.getId()) &&
                macPoolPerCluster.containsDuplicates(macPool.getId()));
    }

    /**
     * Test that there are no overlapping ranges between the specified mac pool and ranges of all other
     * already existing mac pools in the system
     * @param macPool the mac pool to validate against all other existing pools
     * @return a valid result if no overlap found; invalid otherwise
     */
    ValidationResult validateOverlapWithAllCurrentPools(MacPool macPool) {
        final List<MacPool> macPools = getMacPoolDao().getAll();
        return validateOverlapWithOtherPools(macPools, macPool);
    }

    ValidationResult validateOverlapWithOtherPools(List<MacPool> macPools, MacPool macPool) {
        Set<MacPool> overlappingPools = poolsOverlappedByOtherPool(macPools, macPool);
        return ValidationResult.failWith(
            EngineMessage.ACTION_TYPE_FAILED_MAC_POOL_OVERLAPS_WITH_OTHER_POOLS,
            ReplacementUtils.createSetVariableString("macPools", reportOverlaps(overlappingPools))
        ).when(!overlappingPools.isEmpty());
    }


    private String reportOverlaps(Set<MacPool> overlappingPools) {
        StringJoiner overlapReport = new StringJoiner(", ");
        overlappingPools.forEach(pool -> overlapReport.add("'" + pool.getName() + "'"));
        return overlapReport.toString();
    }


    /**
     * Test that there are no overlapping ranges within the specified mac pool
     * @param macPool the mac pool to validate
     * @return a valid result if no overlap found; invalid otherwise
     */
    ValidationResult validateOverlappingRanges(MacPool macPool) {
        return ValidationResult.failWith(EngineMessage.ACTION_TYPE_FAILED_MAC_POOL_CONTAINS_OVERLAPPING_RANGES)
            .when(!filterOverlappingRanges(macPool).isEmpty());
    }
}
