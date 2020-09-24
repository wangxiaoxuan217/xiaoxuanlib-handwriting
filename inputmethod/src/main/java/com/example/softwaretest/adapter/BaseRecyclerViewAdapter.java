package com.example.softwaretest.adapter;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Created by mac on 2020-08-31.
 *
 * @author xiaoxuan
 */
public abstract class BaseRecyclerViewAdapter<T> extends RecyclerView.Adapter<BaseRecyclerViewHolder>
{
    // item view type
    private final int TYPE_NORMAL = 1;
    
    // 保存处于选中状态的itemView的position
    private SparseBooleanArray selectedItems;
    
    private List<T> mDataList = new ArrayList<T>();
    
    private OnItemClickListener<T> mOnItemClickListener;
    
    protected BaseRecyclerViewAdapter()
    {
        selectedItems = new SparseBooleanArray();
    }
    
    public OnItemClickListener<T> getOnItemClickListener()
    {
        return mOnItemClickListener;
    }
    
    @Override
    public int getItemViewType(int position)
    {
        return TYPE_NORMAL;
    }
    
    /**
     * 绑定数据
     *
     * @param holder
     * @param position
     */
    protected abstract void bindData(BaseRecyclerViewHolder holder, int position);
    
    public void setOnItemClickListener(OnItemClickListener li)
    {
        mOnItemClickListener = li;
    }
    
    public List<T> getData()
    {
        return mDataList;
    }
    
    public void onBindData(List<T> data)
    {
        mDataList.clear();
        if (data != null)
        {
            mDataList.addAll(data);
        }
        notifyDataSetChanged();
    }
    
    public T getItemData(int position)
    {
        T res = null;
        // 计算正确的位置
        int realPos = position;
        if (realPos < mDataList.size())
        {
            res = mDataList.get(realPos);
        }
        return res;
    }
    
    // 清除数据
    public void clearData()
    {
        if (mDataList != null)
        {
            mDataList.clear();
            notifyDataSetChanged();
        }
    }
    
    /**
     * itemView的选中状态和非选中状态切换并及时更新UI状态 选中状态调用时就切换为非选中状态，反之对调状态
     *
     * @param position 用户点击的itemView的位置
     */
    public void toggleSelection(int position)
    {
        if (selectedItems.get(position, false))
        {
            selectedItems.delete(position);
        }
        else
        {
            selectedItems.put(position, true);
        }
        /* 这个更新UI会使焦点闪烁一下 */
        // notifyItemChanged(position);
    }
    
    /**
     * 判断这个位置的item是处于选中状态
     *
     * @param position Position of the item to check
     * @return true if the item is selected, false otherwise
     */
    public boolean isSelected(int position)
    {
        return getSelectedItems().contains(position);
    }
    
    // 清除所有Item的选中状态
    public void clearSelection()
    {
        List<Integer> selection = getSelectedItems();
        selectedItems.clear();
        notifyDataSetChanged();
    }
    
    // 获得所有选中状态item的position集合
    private List<Integer> getSelectedItems()
    {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); ++i)
        {
            /* 我们使用keyAt可以取到position，当然也可以使用valueAt取到value值，显然这个集合中的value都为true */
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }
    
    // 获得item的位置
    private int getRealPosition(BaseRecyclerViewHolder holder)
    {
        return holder.getLayoutPosition();
    }
    
    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return createItem(parent, viewType);
    }
    
    @Override
    public void onBindViewHolder(final BaseRecyclerViewHolder holder, int position)
    {
        final int pos = getRealPosition(holder);
        final T data = mDataList.get(pos);
        if (mDataList.size() == 0)
            return;
        bindData(holder, pos);
        if (mOnItemClickListener != null)
        {
            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mOnItemClickListener.onItemClick(pos, true);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    mOnItemClickListener.onItemLongClick(pos, data);
                    return false;
                }
            });
        }
    }
    
    @Override
    public int getItemCount()
    {
        if (mDataList != null && mDataList.size() > 0)
        {
            return mDataList.size();
        }
        return 0;
    }
    
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        // if (manager instanceof GridLayoutManager)
        // {
        // final GridLayoutManager gridManager = ((GridLayoutManager)manager);
        // gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup()
        // {
        // @Override
        // public int getSpanSize(int position)
        // {
        // return gridManager.getSpanCount();
        // }
        // });
        // }
    }
    
    @Override
    public void onViewAttachedToWindow(BaseRecyclerViewHolder holder)
    {
        super.onViewAttachedToWindow(holder);
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams && holder.getLayoutPosition() == 0)
        {
            StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams)lp;
            p.setFullSpan(true);
        }
    }
    
    /**
     * 创建item view
     *
     * @param parent
     * @param viewType
     * @return
     */
    protected abstract BaseRecyclerViewHolder createItem(ViewGroup parent, int viewType);
    
    /**
     * item 点击事件接口
     *
     * @param <T>
     */
    public interface OnItemClickListener<T>
    {
        // 长按监听
        void onItemLongClick(int position, T data);
        
        void onItemClick(int position, boolean flag);
    }
}
