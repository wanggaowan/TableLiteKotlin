package com.keqiang.table.kotlin.model

/**
 * An object pool for recycling of object instances extending Poolable.
 *
 *
 * Cost/Benefit :
 * Cost - The pool can only contain objects extending Poolable.
 * Benefit - The pool can very quickly determine if an object is elligable for storage without iteration.
 * Benefit - The pool can also know if an instance of Poolable is already stored in a different pool instance.
 * Benefit - The pool can grow as needed, if it is empty
 * Cost - However, refilling the pool when it is empty might incur a time cost with sufficiently large capacity.  Set the replenishPercentage to a lower number if this is a concern.
 *
 * Created by Tony Patino on 6/20/16.
 */
class ObjectPool<T : ObjectPool.Poolable> private constructor(
    private var desiredCapacity: Int,
    private val modelObject: T
) {
    /**
     * Returns the id of the given pool instance.
     *
     * @return an integer ID belonging to this pool instance.
     */
    var poolId: Int = 0
        private set
    private var objects: Array<Poolable?>
    private var objectsPointer: Int = 0

    var replenishPercentage: Float = 0.toFloat()
        /**
         * Set the percentage of the pool to replenish on empty.  Valid values are between
         * 0.00f and 1.00f
         *
         * @param percentage a value between 0 and 1, representing the percentage of the pool to replenish.
         */
        set(percentage) {
            var p = percentage
            if (p > 1) {
                p = 1f
            } else if (p < 0f) {
                p = 0f
            }
            field = p
        }

    /**
     * Returns the capacity of this object pool.  Note : The pool will automatically resize
     * to contain additional objects if the user tries to add more objects than the pool's
     * capacity allows, but this comes at a performance cost.
     *
     * @return The capacity of the pool.
     */
    val poolCapacity: Int
        get() = this.objects.size

    /**
     * Returns the number of objects remaining in the pool, for diagnostic purposes.
     *
     * @return The number of objects remaining in the pool.
     */
    val poolCount: Int
        get() = this.objectsPointer + 1

    init {
        if (desiredCapacity <= 0) {
            throw IllegalArgumentException("Object Pool must be instantiated with a capacity greater than 0!")
        }
        this.objects = arrayOfNulls(this.desiredCapacity)
        this.objectsPointer = 0
        this.replenishPercentage = 1.0f
        this.refillPool()
    }

    private fun refillPool() {
        this.refillPool(this.replenishPercentage)
    }

    private fun refillPool(percentage: Float) {
        var portionOfCapacity = (desiredCapacity * percentage).toInt()

        if (portionOfCapacity < 1) {
            portionOfCapacity = 1
        } else if (portionOfCapacity > desiredCapacity) {
            portionOfCapacity = desiredCapacity
        }

        for (i in 0 until portionOfCapacity) {
            this.objects[i] = modelObject.instantiate()
        }
        objectsPointer = portionOfCapacity - 1
    }

    /**
     * Returns an instance of Poolable.  If get() is called with an empty pool, the pool will be
     * replenished.  If the pool capacity is sufficiently large, this could come at a performance
     * cost.
     *
     * @return An instance of Poolable object T
     */
    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun get(): T {
        if (this.objectsPointer == -1 && this.replenishPercentage > 0.0f) {
            this.refillPool()
        }

        val result = objects[this.objectsPointer] as T
        result.currentOwnerId = Poolable.NO_OWNER
        this.objectsPointer--
        return result
    }

    /**
     * Recycle an instance of Poolable that this pool is capable of generating.
     * The T instance passed must not already exist inside this or any other ObjectPool instance.
     *
     * @param object An object of type T to recycle
     */
    @Synchronized
    fun recycle(`object`: T) {
        if (`object`.currentOwnerId != Poolable.NO_OWNER) {
            if (`object`.currentOwnerId == this.poolId) {
                throw IllegalArgumentException("The object passed is already stored in this pool!")
            } else {
                throw IllegalArgumentException("The object to recycle already belongs to poolId " + `object`.currentOwnerId + ".  Object cannot belong to two different pool instances simultaneously!")
            }
        }

        this.objectsPointer++
        if (this.objectsPointer >= objects.size) {
            this.resizePool()
        }

        `object`.currentOwnerId = this.poolId
        `object`.recycle()
        objects[this.objectsPointer] = `object`

    }

    /**
     * Recycle a List of Poolables that this pool is capable of generating.
     * The T instances passed must not already exist inside this or any other ObjectPool instance.
     *
     * @param objects A list of objects of type T to recycle
     */
    @Synchronized
    fun recycle(objects: List<T>) {
        while (objects.size + this.objectsPointer + 1 > this.desiredCapacity) {
            this.resizePool()
        }
        val objectsListSize = objects.size

        // Not relying on recycle(T object) because this is more performance.
        for (i in 0 until objectsListSize) {
            val `object` = objects[i]
            if (`object`.currentOwnerId != Poolable.NO_OWNER) {
                if (`object`.currentOwnerId == this.poolId) {
                    throw IllegalArgumentException("The object passed is already stored in this pool!")
                } else {
                    throw IllegalArgumentException("The object to recycle already belongs to poolId " + `object`.currentOwnerId + ".  Object cannot belong to two different pool instances simultaneously!")
                }
            }

            `object`.currentOwnerId = this.poolId
            `object`.recycle()
            this.objects[this.objectsPointer + 1 + i] = `object`
        }
        this.objectsPointer += objectsListSize
    }

    private fun resizePool() {
        val oldCapacity = this.desiredCapacity
        this.desiredCapacity *= 2
        val temp = arrayOfNulls<Poolable>(this.desiredCapacity)
        System.arraycopy(this.objects, 0, temp, 0, oldCapacity)
        this.objects = temp
    }

    abstract class Poolable {
        internal var currentOwnerId = NO_OWNER

        /**
         * create no Param instance
         */
        abstract fun instantiate(): Poolable

        /**
         * recycle clear data
         */
        abstract fun recycle()

        companion object {
            var NO_OWNER: Int = -1
        }
    }

    companion object {
        private var ids = 0

        /**
         * Returns an ObjectPool instance, of a given starting capacity, that recycles instances of a given Poolable object.
         *
         * @param withCapacity A positive integer value.
         * @param object       An instance of the object that the pool should recycle.
         */
        @Synchronized
        fun <P : Poolable> create(withCapacity: Int, `object`: P): ObjectPool<P> {
            val result = ObjectPool(withCapacity, `object`)
            result.poolId = ids
            ids++
            return result
        }
    }
}