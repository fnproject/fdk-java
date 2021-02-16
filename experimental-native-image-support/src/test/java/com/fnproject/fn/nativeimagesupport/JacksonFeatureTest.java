package com.fnproject.fn.nativeimagesupport;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 16/02/2021.
 * <p>
 * (c) 2021 Oracle Corporation
 */
public class JacksonFeatureTest {


    @Test
    public void shouldWalkAnnotations() {
        assertThat(JacksonFeature.expandAnnotationReferencesForClass(AbstractBase.class)).containsExactly(ConcreteSubType.class);
        assertThat(JacksonFeature.expandAnnotationReferencesForClass(InterfaceBase.class)).containsExactly(InterfaceImpl.class);
        assertThat(JacksonFeature.expandAnnotationReferencesForClass(UsesJacksonFeatures.class)).containsExactly(UsesJacksonFeatures.Builder.class);
        assertThat(JacksonFeature.expandAnnotationReferencesForClass(AnnotationsOnFields.class)).containsExactly(BigInteger.class);
        assertThat(JacksonFeature.expandAnnotationReferencesForClass(AnnotationsOnMethods.class)).containsExactly(BigInteger.class);
    }



    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ConcreteSubType.class, name = "c1")
    })
    public abstract static class AbstractBase {

    }

    public static class ConcreteSubType extends AbstractBase {
        int id;
    }

    public static class InterfaceImpl implements InterfaceBase {
    }

    /**
     * Created on 15/02/2021.
     * <p>
     * (c) 2021 Oracle Corporation
     */
    @JsonDeserialize(as = InterfaceImpl.class)
    public interface InterfaceBase {
    }


    @JsonDeserialize(builder = UsesJacksonFeatures.Builder.class)
    public static class UsesJacksonFeatures {
        @JsonPOJOBuilder
        public static class Builder {
        }
    }

    public static class AnnotationsOnFields {
        @JsonSerialize(as = BigInteger.class)
        private int number;
    }


    public static class AnnotationsOnMethods {
        @JsonSerialize(as = BigInteger.class)
        public int getNumber() {
            return 1;
        }
    }
}
