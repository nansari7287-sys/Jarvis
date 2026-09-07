package com.example.jarvis.ui
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jarvis.R
import com.example.jarvis.models.Message
class ChatAdapter:RecyclerView.Adapter<ChatAdapter.VH>(){private val items=mutableListOf<Message>();fun submitList(m:List<Message>){items.clear();items.addAll(m);notifyDataSetChanged()};override fun getItemViewType(p:Int)=if(items[p].isUser)1 else 0;override fun onCreateViewHolder(p:ViewGroup,t:Int)=VH(LayoutInflater.from(p.context).inflate(if(t==1)R.layout.item_user_message else R.layout.item_jarvis_message,p,false));override fun onBindViewHolder(h:VH,p:Int){h.text.text=items[p].text};override fun getItemCount()=items.size;class VH(v:View):RecyclerView.ViewHolder(v){val text:TextView=v.findViewById(R.id.messageText)}}
