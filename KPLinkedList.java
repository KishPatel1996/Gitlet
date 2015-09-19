import java.io.Serializable;

public class KPLinkedList<E> implements Serializable {


    private E head;
    private KPLinkedList<E> bottom;
    private static final long serialVersionUID = 42L;



    public KPLinkedList(E h, KPLinkedList<E> n) {
        head = h;
        bottom = n;

    }

    public E getHead() {
        return head;
    }

    public KPLinkedList<E> next() {
        return bottom;
    }
    public KPLinkedList<E> getBottom() {
        return bottom;
    }

    public void setBottom(KPLinkedList<E> bottom) {
        this.bottom = bottom;
    }

    public void setHead(E head) {
        this.head = head;
    }


}
