package java_src

import java.lang.ref.WeakReference

final class Cache[K, V >: Null](newValue: K => V) {

  private[this] val map = new java.util.WeakHashMap[K, WeakReference[V]]

  def apply(name: K): V = synchronized{
    def cached(): V = {
      val reference = map get name
      if (reference == null) null
      else reference.get
    }
    def updateCache(): V = {
      val res = cached()
      if (res != null) res
      else {
        map remove name
        val sym = newValue(name)
        map.put(name, new WeakReference(sym))
        sym
      }
    }

    val res = cached()
    if (res == null) updateCache()
    else res
  }
}
