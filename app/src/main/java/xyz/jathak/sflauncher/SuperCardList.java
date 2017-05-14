package xyz.jathak.sflauncher;

import android.support.v7.widget.RecyclerView;
import com.daimajia.swipe.SwipeLayout;

import java.util.ArrayList;

public class SuperCardList extends ArrayList<Card> {

    public RecyclerView.Adapter adapter;
    public MainActivity ctx;
    public RecyclerView listView;
    public SFWidgetHost widgetHost;

    public SuperCardList(MainActivity ctx, RecyclerView listView){
        super();
        this.ctx = ctx;
        this.listView = listView;
    }

    @Override
    public boolean remove(Object c){
        if(contains(c)){
            int index = indexOf(c);
            deleteCard(index);
            return true;
        }else return false;
    }

    @Override
    public Card remove(int index){
        if(index<0||index>=size())return null;
        Card c = get(index);
        deleteCard(index);
        return c;
    }

    public void moveToTop(Card c){
        int position = indexOf(c);
        super.remove(c);
        if(adapter!=null){
            adapter.notifyItemRemoved(position);
        }
        add(1, c);
        if(adapter!=null){
            adapter.notifyDataSetChanged();
        }
    }

    public void moveToBottom(Card c){
        int position = indexOf(c);
        super.remove(c);
        if(adapter!=null){
            adapter.notifyItemRemoved(position);
        }
        add(c);
        if(adapter!=null){
            adapter.notifyDataSetChanged();
        }
    }

    private void deleteCard(int index){
        final Card c = get(index);
        super.remove(c);
        if(adapter!=null){
            MainActivity.saveCards(ctx);
            adapter.notifyItemRemoved(index);
            if(index==1){
                adapter.notifyItemChanged(1);
            }
            else if(index==size()&&size()>0)adapter.notifyItemChanged(index-1);
        }
        if(c instanceof Card.Widget){
            int id = ((Card.Widget)c).id;
            if(widgetHost!=null){
                widgetHost.deleteAppWidgetId(id);
            }
        }
    }

    public boolean swap(final int a, final int b){
        if(a<0||a>=size()||b<0||b>=size()||a==b)return false;
        if(a>b) return swap(b, a);
        final SwipeLayout av = get(a).getWrapper();
        final SwipeLayout bv = get(b).getWrapper();
        av.close(true);
        bv.close(true);
        super.add(b, super.remove(a));
        if(adapter!=null){
            MainActivity.saveCards(ctx);
            adapter.notifyItemMoved(a, b);
            adapter.notifyItemChanged(a);
            adapter.notifyItemChanged(b);
        }
        return true;
    }

    @Override
    public void add(int index, Card c){
        super.add(index, c);
        MainActivity.saveCards(ctx);
        if(adapter!=null){
            adapter.notifyItemInserted(index);
            if(index>1)adapter.notifyItemChanged(index-1);
            else if(index==1&&size()>2)adapter.notifyItemChanged(2);
        }
    }

    @Override
    public boolean add(Card c){
        add(size(),c);
        return true;
    }

    public boolean quickAdd(Card c){
        boolean result = super.add(c);
        if(listView!=null){
            listView.scrollToPosition(0);
        }
        return result;
    }
}
