package no.dcat.shared;

import lombok.Data;

import java.util.Date;

@Data
public class HarvestMetadata {
    private Date firstHarvested;

    private Date lastHarvested;

    private Date lastChanged;
}
