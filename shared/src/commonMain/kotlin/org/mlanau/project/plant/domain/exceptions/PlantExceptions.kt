package org.mlanau.project.plant.domain.exceptions

sealed class DomainException(message: String) : Exception(message)

class EmptyPlantNameException : DomainException("Plant name cannot be empty")

class InvalidCareRuleDateRangeException : DomainException("End date cannot be before start date")

class InvalidRecurrenceException : DomainException("Recurrence interval must be greater than zero")
