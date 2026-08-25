package com.example.school.register

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class InternalTokenHolder(@ConfigProperty(name = "app.internal.token") val value: String)
