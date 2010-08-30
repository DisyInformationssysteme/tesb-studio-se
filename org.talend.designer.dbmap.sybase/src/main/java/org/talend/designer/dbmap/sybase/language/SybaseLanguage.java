// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.dbmap.sybase.language;

import org.talend.designer.dbmap.language.AbstractDbLanguage;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id: JavaLanguage.java 1877 2007-02-06 17:16:43Z amaumont $
 * 
 */
public class SybaseLanguage extends AbstractDbLanguage {

    /**
     * DOC amaumont PerlLanguage constructor comment.
     */
    public SybaseLanguage() {
        super(new SybaseOperatorsManager());
    }

}
