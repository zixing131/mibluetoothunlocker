package zixing.bluetooth.unlocker.adapter.base;

import android.view.View;

import java.util.LinkedHashMap;

/**
 * Author:沫湮
 * Date:2017/1/19
 * Time:9:32
 * QQ:269139812
 * E-mail:269139812@qq.com
 */

public class Tools {
    public static <T extends View> T get(View view, int id) {
        LinkedHashMap viewHolder = (LinkedHashMap) view.getTag();
        if (viewHolder == null) {
            viewHolder = new LinkedHashMap<>();
            view.setTag(viewHolder);
        }
        View childView = (View) viewHolder.get(id);
        if (childView == null) {
            childView = view.findViewById(id);
            viewHolder.put(id, childView);
        }
        return (T)childView;
    }
}
