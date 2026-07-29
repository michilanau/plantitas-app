package org.mlanau.project.plant.domain.exceptions

sealed class DomainException(message: String) : Exception(message)

class EmptyPlantNameException : DomainException("Plant name cannot be empty")
