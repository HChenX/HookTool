package HookTool.extensions.java.lang.String;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class StringExt {
    public static void echo(@This String thiz) {
        System.out.println(thiz);
    }
}