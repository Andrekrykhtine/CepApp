package com.example.cepapplication.data.local

class LocalStorageException(cause: Throwable) :
    RuntimeException("Falha no armazenamento local", cause)
