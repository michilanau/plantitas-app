package org.mlanau.project.plant.domain.exception

class MissingPersistedIdException : DomainException("Repository returned a record without an id")
