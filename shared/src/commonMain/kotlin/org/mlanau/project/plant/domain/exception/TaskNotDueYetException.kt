package org.mlanau.project.plant.domain.exception

class TaskNotDueYetException : DomainException("A scheduled task cannot be completed before it is due")
