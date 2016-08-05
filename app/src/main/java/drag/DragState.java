package drag;
public class DragState {
    private Object mItem;
    private String mItemId;
    public DragState(Object mItem, String mItemId) {
        this.mItem = mItem;
        this.mItemId = mItemId;
    }

    public Object getItem() {
        return this.mItem;
    }

    public String getItemId() {
        return this.mItemId;
    }
}
