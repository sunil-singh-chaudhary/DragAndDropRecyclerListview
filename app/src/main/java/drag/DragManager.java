package drag;

import android.graphics.PointF;
import android.support.v7.widget.RecyclerView;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewConfiguration;

import static java.lang.Float.MIN_VALUE;
public class DragManager implements View.OnDragListener {

    private static final int SCROLL_DIR_NONE = 0;
    private static final float SCROLL_THRESHOLD = 0.3f;
    private static final float SCROLL_AMOUNT_COEFF = 25;
    private static final int SCROLL_DIR_UP = (1 << 0);
    private static final int SCROLL_DIR_DOWN = (1 << 1);
    private static final float SCROLL_TOUCH_SLOP_MULTIPLY = 1.0f;


    private RecyclerView recyclerView;
    private DragListener dragListener;

    private int originPosition;
    private int fromPosition = -1;
    private boolean canDrag = false;

    private float mDisplayDensity;
    private int mLastTouchX;
    private int mLastTouchY;
    private int mDragStartTouchX;
    private int mDragStartTouchY;
    private int mDragMinTouchX;
    private int mDragMinTouchY;
    private int mDragMaxTouchX;
    private int mDragMaxTouchY;
    private int mScrollDirMask = SCROLL_DIR_NONE;
    private int mScrollTouchSlop;

    private final PointF nextMoveTouchPoint = new PointF(MIN_VALUE, MIN_VALUE);

    public DragManager(RecyclerView recyclerView, DragListener dragListener) {
        this.recyclerView = recyclerView;
        mDisplayDensity = recyclerView.getResources().getDisplayMetrics().density;
        int mTouchSlop = ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop();
        mScrollTouchSlop = (int) (mTouchSlop * SCROLL_TOUCH_SLOP_MULTIPLY);
        this.dragListener = dragListener;
    }


    @Override
    public boolean onDrag(View v, DragEvent event) {
        if (!(event.getLocalState() instanceof DragState)) {
            return false;
        }
        final DragState dragState = (DragState) event.getLocalState();
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                onDragStart(event, dragState);
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                break;
            case DragEvent.ACTION_DRAG_LOCATION:

                if (!canDrag) {
                    return true;
                }
                handleActionMoveWhileDragging(event);

                float x = event.getX();
                float y = event.getY();
                if (fromPosition == -1) {
                    fromPosition = dragListener.getPosition(dragState.getItem());
                }
                int toPosition = RecyclerView.NO_POSITION;

                final View child = recyclerView.findChildViewUnder(x, y);
                if (child != null) {
                    toPosition = recyclerView.getChildViewHolder(child).getAdapterPosition();
                }
                if (fromPosition != toPosition) {
                    swapItems(event);
                }
                break;
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_ENDED:
                dragListener.notifyItemChange(dragListener.getPosition(dragState.getItem()), DragListener.NO_ID);
                fromPosition = -1;
                mScrollDirMask = SCROLL_DIR_NONE;
                prepareForNextMove();
                break;
            case DragEvent.ACTION_DROP:
                if (!canDrag) {
                    return true;
                }
                int targetPosition = dragListener.getPosition(dragState.getItem());
                if (targetPosition != originPosition) {
                    dragListener.onDragEnd(targetPosition);
                }
                break;
        }
        return true;
    }

    public void setCanDrag(boolean canDrag) {
        this.canDrag = canDrag;
    }

    private void onDragStart(DragEvent event, DragState dragState) {
        dragListener.notifyItemChange(dragListener.getPosition(dragState.getItem()), dragState.getItemId());
        mLastTouchX = (int) (event.getX() + 0.5f);
        mLastTouchY = (int) (event.getY() + 0.5f);
        mDragStartTouchY = mDragMinTouchY = mDragMaxTouchY = mLastTouchY;
        mDragStartTouchX = mDragMinTouchX = mDragMaxTouchX = mLastTouchX;
        mScrollDirMask = SCROLL_DIR_NONE;
        originPosition = dragListener.getPosition(dragState.getItem());
    }

    private void swapItems(final DragEvent event) {
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        boolean canNextSwap = nextMoveTouchPoint.equals(MIN_VALUE, MIN_VALUE);
        nextMoveTouchPoint.set(event.getX(), event.getY());
        if (canNextSwap) {
            animator.isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                @Override
                public void onAnimationsFinished() {
                    if (nextMoveTouchPoint.equals(MIN_VALUE, MIN_VALUE)) {
                        return;
                    }
                    View child = recyclerView.findChildViewUnder(nextMoveTouchPoint.x, nextMoveTouchPoint.y);
                    if (child != null) {
                        final int toPosition = recyclerView.getChildViewHolder(child).getAdapterPosition();

                        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                            return;
                        }
                        if (fromPosition == 0 || toPosition == 0) {
                            recyclerView.scrollToPosition(0);
                        }
                        if (fromPosition != toPosition) {
                            if (dragListener.onMoveItem(fromPosition, toPosition)) {
                                fromPosition = toPosition;
                            }
                        }
                    }
                    prepareForNextMove();
                }
            });
        }
    }

    void prepareForNextMove() {
        nextMoveTouchPoint.set(MIN_VALUE, MIN_VALUE);
    }

    private void handleActionMoveWhileDragging(DragEvent e) {
        mLastTouchX = (int) (e.getX() + 0.5f);
        mLastTouchY = (int) (e.getY() + 0.5f);
        mDragMinTouchX = Math.min(mDragMinTouchX, mLastTouchX);
        mDragMinTouchY = Math.min(mDragMinTouchY, mLastTouchY);
        mDragMaxTouchX = Math.max(mDragMaxTouchX, mLastTouchX);
        mDragMaxTouchY = Math.max(mDragMaxTouchY, mLastTouchY);
        // update drag direction mask
        updateDragDirectionMask();
        handleScroll();
    }

    private void updateDragDirectionMask() {
        if (((mDragStartTouchY - mDragMinTouchY) > mScrollTouchSlop) ||
                ((mDragMaxTouchY - mLastTouchY) > mScrollTouchSlop)) {
            mScrollDirMask |= SCROLL_DIR_UP;
        }
        if (((mDragMaxTouchY - mDragStartTouchY) > mScrollTouchSlop) ||
                ((mLastTouchY - mDragMinTouchY) > mScrollTouchSlop)) {
            mScrollDirMask |= SCROLL_DIR_DOWN;
        }
    }


    private void handleScroll() {
        final int edge = recyclerView.getHeight();
        if (edge == 0) {
            return;
        }
        final int mask = mScrollDirMask;
        final float invEdge = (1.0f / edge);
        final float normalizedTouchPos = (mLastTouchY) * invEdge;
        final float threshold = SCROLL_THRESHOLD;
        final float invThreshold = (1.0f / threshold);
        final float centerOffset = normalizedTouchPos - 0.5f;
        final float absCenterOffset = Math.abs(centerOffset);
        final float acceleration = Math.max(0.0f, threshold - (0.5f - absCenterOffset)) * invThreshold;
        int scrollHeight = (int) Math.signum(centerOffset) * (int) (SCROLL_AMOUNT_COEFF * mDisplayDensity * acceleration + 0.5f);
        // apply mask
        if (scrollHeight > 0) {
            if ((mask & (SCROLL_DIR_DOWN)) == 0) {
                scrollHeight = 0;
            }
        } else if (scrollHeight < 0) {
            if ((mask & (SCROLL_DIR_UP)) == 0) {
                scrollHeight = 0;
            }
        }

        if (scrollHeight != 0) {
            recyclerView.scrollBy(0, scrollHeight);
        }

    }
}
