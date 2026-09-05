package com.empire.forest.util

class BoundedLRUAccessor<T>(
    private val maxLength: Int
) {
    private val _buffer : MutableList<T> = mutableListOf()
    val buffer : List<T> = _buffer

    fun emplaceItem(item: T) : Result<T> {
        var result : Result<T> = Result.Success<T>()
        if (_buffer.size >= maxLength) {
            result = Result.Replaced(_buffer.removeLast())
        }
        _buffer.add(0, item)
        return result
    }

    fun removeItem(item: T) : Boolean =
        _buffer.remove(item)

    fun clear() {
        _buffer.clear()
    }

    sealed interface Result<T> {
        class Success<T> : Result<T>
        data class Replaced<T>(val item: T) : Result<T>
    }
}