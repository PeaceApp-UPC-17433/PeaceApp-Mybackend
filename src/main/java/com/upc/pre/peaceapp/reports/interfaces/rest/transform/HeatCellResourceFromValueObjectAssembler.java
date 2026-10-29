package com.upc.pre.peaceapp.reports.interfaces.rest.transform;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.HeatCell;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.HeatCellResource;

public class HeatCellResourceFromValueObjectAssembler {

    public static HeatCellResource toResourceFromValueObject(HeatCell cell) {
        return new HeatCellResource(cell.latitude(), cell.longitude(), cell.count(), cell.riskLevel());
    }
}
