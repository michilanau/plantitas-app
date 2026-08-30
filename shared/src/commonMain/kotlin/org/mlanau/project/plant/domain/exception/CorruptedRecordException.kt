package org.mlanau.project.plant.domain.exception

class CorruptedRecordException(recordId: Int, detail: String) :
    DomainException("Corrupted record #$recordId: $detail")
