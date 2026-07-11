package com.sysu.edu.view

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.sysu.edu.api.Config
import java.util.Collections

abstract class RecyclerAdapter<T> : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
	val data: ArrayList<T> = ArrayList()
	var listener: AdapterListener? = null
	protected var config: Config? = null
	override fun getItemCount(): Int = data.size
	
	/*@NonNull
   @Override
   public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
	   ViewBinding binding = ViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
	   if (listener != null) listener.onCreate(this, binding);
	   return new RecyclerView.ViewHolder(binding.getRoot()) {
	   };
   }*/
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		listener?.onBind(this, holder, position)
	}
	
	fun add(item: T) {
		data.add(item)
		notifyItemInserted(itemCount - 1)
		notifyItemChanged(itemCount - 2)
	}
	
	fun remove(position: Int) {
		data.removeAt(position)
		notifyItemRemoved(position)
		notifyItemRangeChanged(position, position - 1)
		notifyItemRangeChanged(position, itemCount - position)
	}
	
	fun clear() {
		val temp = itemCount
		data.clear()
		notifyItemRangeRemoved(0, temp)
	}
	
	fun get(position: Int): T = if (position in 0..<itemCount) data[position] else null!!
	fun set(d: MutableList<out T>) {
		clear()
		data.addAll(d)
		notifyItemRangeChanged(0, itemCount)
	}
	
	fun swap(position1: Int, position2: Int) {
		Collections.swap(data, position1, position2)
		notifyItemMoved(position1, position2)
	}
	
	fun setParams(config: Config?) {
		this.config = config
	}
	
	fun forEach(action: (T) -> Unit) {
		data.forEach(action)
	}
}

internal abstract class Recycler2Adapter<T, V : ViewDataBinding?> :
	RecyclerView.Adapter<RecyclerViewHolder<V?>?>() {
	protected val data = ArrayList<T?>()
	protected var listener: Adapter2Listener<V?>? = null
	protected var config: Config? = null
	override fun getItemCount(): Int {
		return data.size
	}
	
	//    @NonNull
	//    @Override
	//    public RecyclerViewHolder<V> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
	//        V binding = V.inflate(LayoutInflater.from(parent.getContext()), parent, false);
	//        if (listener != null) listener.onCreate(this, binding);
	//        return new RecyclerViewHolder<>(binding);
	//    }
	override fun onBindViewHolder(holder: RecyclerViewHolder<V?>, position: Int) {
		if (listener != null) listener!!.onBind(this, holder, position)
	}
	
	fun add(item: T?) {
		data.add(item)
		notifyItemInserted(itemCount - 1)
	}
	
	fun remove(position: Int) {
		data.removeAt(position)
		notifyItemRemoved(position)
		notifyItemRangeChanged(position, position - 1)
		notifyItemRangeChanged(position, itemCount - position)
	}
	
	fun clear() {
		val temp = itemCount
		data.clear()
		notifyItemRangeRemoved(0, temp)
	}
	
	fun get(position: Int): T? {
		return if (position in 0..<itemCount) data[position] else null
	}
	
	fun set(d: MutableList<out T?>) {
		clear()
		data.addAll(d)
		notifyItemRangeInserted(0, itemCount)
	}
	
	fun swap(position1: Int, position2: Int) {
		Collections.swap(data, position1, position2)
		notifyItemMoved(position1, position2)
	}
	
	fun setParams(config: Config?) {
		this.config = config
	}
}
