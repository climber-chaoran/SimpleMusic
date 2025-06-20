package org.litepal.tablemanager.typechange;

/* loaded from: classes.dex */
public class BooleanOrm extends OrmChange {
    @Override // org.litepal.tablemanager.typechange.OrmChange
    public String object2Relation(String str) {
        if (str == null) {
            return null;
        }
        if (str.equals("boolean") || str.equals("java.lang.Boolean")) {
            return "integer";
        }
        return null;
    }
}
