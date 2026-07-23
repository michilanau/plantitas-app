package org.mlanau.project.plant.domain.error

sealed class DomainException(message: String) : Exception(message)

class EmptyPlantNameException : DomainException("Plant name cannot be empty")
