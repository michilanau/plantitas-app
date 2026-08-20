package org.mlanau.project.plant.domain.exceptions

sealed class DomainException(message: String) : Exception(message)

class EmptyPlantNameException : DomainException("Plant name cannot be empty")

class InvalidCareRuleDateRangeException : DomainException("End date cannot be before start date")

class InvalidRecurrenceException : DomainException("Recurrence interval must be greater than zero")

class PlantNotFoundException : DomainException("Plant not found")

/** A [org.mlanau.project.plant.domain.model.CareLog] can only record care that has already
 * happened — logging one dated after the current instant would let the user "pre-complete" a
 * future task, which is exactly what the relative-anchor scheduling model must not allow. */
class FutureCareLogException : DomainException("Care cannot be logged in the future")
