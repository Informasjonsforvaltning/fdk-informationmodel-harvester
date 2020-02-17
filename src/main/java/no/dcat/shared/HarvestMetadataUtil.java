package no.dcat.shared;

import java.util.Date;

/*
HarvestMetadataUtil is class for logic for managing HarvestMetadata objects, which itself are Anemic DTO
*/
public class HarvestMetadataUtil {

    public static HarvestMetadata createOrUpdate(HarvestMetadata oldMetadata, Date harvestDate, boolean hasChanged) {

        HarvestMetadata metadata = new HarvestMetadata();

        if (oldMetadata != null && oldMetadata.getFirstHarvested() != null) {
            metadata.setFirstHarvested(oldMetadata.getFirstHarvested());
        } else
            metadata.setFirstHarvested(harvestDate);

        metadata.setLastHarvested(harvestDate);

        if (!hasChanged) {
            metadata.setLastChanged(harvestDate);
        }

        return metadata;
    }

}
