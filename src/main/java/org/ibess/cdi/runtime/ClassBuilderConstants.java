package org.ibess.cdi.runtime;

import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.internal.$Instantiator;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Type.*;

/**
 * @author ibessonov
 */
interface ClassBuilderConstants {

    String $                       = "$";

    String CDI_SUFFIX              = "$Cdi";
    String I_SUFFIX                = "$I";

    String CONTEXT_F_NAME          = "$c";
    String DESCRIPTOR_F_NAME       = "$d";
    String INSTANCE_F_NAME         = "$i";
    String DESCRIPTOR_CLASS_F_NAME = "c";

    String INIT_M_NAME             = "<init>";
    String CLINIT_M_NAME           = "<clinit>";
    String DESCRIPTOR_M_NAME       = "$";
    String DESCRIPTOR0_M_NAME      = "$0";
    String CONSTRUCT_M_NAME        = "$construct";
    String CREATE_M_NAME           = "$create";
    String LOOKUP_M_NAME           = "$lookup";
    String UNSCOPED_M_NAME         = "$unscoped";
    String SINGLETON_M_NAME        = "$singleton";
    String STATELESS_M_NAME        = "$stateless";
    String REQUEST_M_NAME          = "$request";

    String CONTEXT_INTERNAL        = getInternalName($Context.class);
    String DESCRIPTOR_INTERNAL     = getInternalName($Descriptor.class);
    String CDI_OBJECT_INTERNAL     = getInternalName($CdiObject.class);
    String INSTANTIATOR_INTERNAL   = getInternalName($Instantiator.class);
    String OBJECT_INTERNAL         = getInternalName(Object.class);

    String CONTEXT_DESCR           = getDescriptor($Context.class);
    String DESCRIPTOR_DESCR        = getDescriptor($Descriptor.class);
    String DESCRIPTOR_A_DESCR      = getDescriptor($Descriptor[].class);
    String CDI_OBJ_DESCR           = getDescriptor($CdiObject.class);
    String OBJECT_DESCR            = getDescriptor(Object.class);
    String CLASS_DESCR             = getDescriptor(Class.class);

    Type   CONTEXT_TYPE            = getType(CONTEXT_DESCR);
    Type   DESCRIPTOR_TYPE         = getType(DESCRIPTOR_DESCR);
    Type   DESCRIPTOR_A_TYPE       = getType(DESCRIPTOR_A_DESCR);
    Type   CDI_OBJ_TYPE            = getType(CDI_OBJ_DESCR);
    Type   OBJECT_TYPE             = getType(OBJECT_DESCR);
    Type   CLASS_TYPE              = getType(CLASS_DESCR);

    String INIT_M_DESCR            = getMethodDescriptor(VOID_TYPE);
    String CREATE_M_DESCR          = getMethodDescriptor(CDI_OBJ_TYPE, CONTEXT_TYPE, DESCRIPTOR_A_TYPE);
    String LOOKUP_M_DESCR          = getMethodDescriptor(OBJECT_TYPE, DESCRIPTOR_TYPE);
    String UNSCOPED_M_DESCR        = getMethodDescriptor(OBJECT_TYPE, CLASS_TYPE);
    String SINGLETON_M_DESCR       = LOOKUP_M_DESCR;
    String STATELESS_M_DESCR       = LOOKUP_M_DESCR;
    String REQUEST_M_DESCR         = LOOKUP_M_DESCR;
    String DESCRIPTOR_M_DESCR      = getMethodDescriptor(DESCRIPTOR_TYPE, CLASS_TYPE, DESCRIPTOR_A_TYPE);
    String DESCRIPTOR0_M_DESCR     = getMethodDescriptor(DESCRIPTOR_TYPE, CLASS_TYPE);
}
