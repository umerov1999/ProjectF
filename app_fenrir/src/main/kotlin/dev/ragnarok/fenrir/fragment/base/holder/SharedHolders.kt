package dev.ragnarok.fenrir.fragment.base.holder

import android.util.SparseArray
import java.lang.ref.WeakReference

class SharedHolders<T : IdentificableHolder>(supportManyHoldersForEntity: Boolean) {
    val cache: SparseArray<MutableSet<WeakReference<T>>> = SparseArray(0)
    private val mSupportManyHoldersForEntity: Boolean = supportManyHoldersForEntity
    fun findOneByEntityId(entityId: Int): T? {
        val weakReferences: Set<WeakReference<T>> = cache[entityId] ?: return null
        for (weakReference in weakReferences) {
            val holder = weakReference.get()
            if (holder != null) {
                return holder
            }
        }
        return null
    }

    fun findHolderByHolderId(holderId: Int): T? {
        for (i in 0 until cache.size()) {
            val key = cache.keyAt(i)
            val holders: Set<WeakReference<T>> = cache[key]
            for (reference in holders) {
                val holder = reference.get() ?: continue
                if (holder.holderId == holderId) {
                    return holder
                }
            }
        }
        return null
    }

    fun put(entityId: Int, holder: T) {
        //Logger.d(TAG, "TRY to put holder, entityId: " + entityId);
        var success = false
        for (i in 0 until cache.size()) {
            val key = cache.keyAt(i)
            val holders = cache[key]
            val mustHaveInThisSet = entityId == key
            val iterator = holders.iterator()
            while (iterator.hasNext()) {
                val reference = iterator.next()
                val h = reference.get()
                if (h == null) {
                    //Logger.d(TAG, "WEAK reference expire, remove");
                    iterator.remove()
                    continue
                }
                if (holder === h) {
                    if (!mustHaveInThisSet) {
                        //Logger.d(TAG, "THIS holder should not be here, remove");
                        iterator.remove()
                    } else {
                        success = true
                        //Logger.d(TAG, "THIS holder alredy exist there");
                    }
                } else {
                    if (!mSupportManyHoldersForEntity && mustHaveInThisSet) {
                        //Logger.d(TAG, "CACHE not support many holders for entity, remove other holder");
                        iterator.remove()
                    }
                }
            }
            if (mustHaveInThisSet && !success) {
                //Logger.d(TAG, "SET for entity already exist, but holder not found, added");
                val reference = WeakReference(holder)
                holders.add(reference)
                success = true
            }
        }
        if (!success) {
            //Logger.d(TAG, "SET for entity does not exist yes, created and added");
            val set: MutableSet<WeakReference<T>> = HashSet(1)
            set.add(WeakReference(holder))
            cache.put(entityId, set)
        }

        //printDump();
    }

    /*private void printDump(){
        Logger.d(TAG, "DUMP START ############################");
        for(int i = 0; i < mHoldersCache.size(); i++){
            int key = mHoldersCache.keyAt(i);

            Set<WeakReference<T>> holders = mHoldersCache.get(key);

            for(WeakReference<T> weakReference : holders){
                T holder = weakReference.get();

                Logger.d(TAG, "DUMP, entityId: " + key + ", holder: " + (holder == null ? "null" : String.valueOf(holder.getHolderId())));
            }
        }

        Logger.d(TAG, "DUMP END ##############################");
    }*/
    fun release() {
        cache.clear()
    }

}