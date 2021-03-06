package org.nlab.xml.stream.matcher;

import org.apache.commons.lang3.Validate;
import org.jooq.lambda.fi.util.function.CheckedFunction;
import org.nlab.xml.stream.context.StreamContext;
import org.nlab.xml.stream.predicate.AttributeValuesPredicate;
import org.nlab.xml.stream.predicate.Predicates;


public final class EventMatchers {

    private EventMatchers() {
    }

    public static EventMatcher css(String query, CheckedFunction<StreamContext,Boolean> consumer){
        Validate.notBlank(query);
        return new EventMatcher(Predicates.css(query), consumer);
    }


    public static EventMatcher elements(String[] elements, CheckedFunction<StreamContext,Boolean> consumer){
        Validate.notEmpty(elements);
        return new EventMatcher(Predicates.elements(elements), consumer);
    }

    public static EventMatcher element(String element, CheckedFunction<StreamContext,Boolean> consumer){
        Validate.notBlank(element);
        return new EventMatcher(Predicates.elements(element), consumer);
    }

    public static EventMatcher attribute(String[] attributes, CheckedFunction<StreamContext,Boolean> consumer){
        Validate.notEmpty(attributes);
        return new EventMatcher(Predicates.attributes(attributes), consumer);
    }

    public static EventMatcher attribute(String attribute, CheckedFunction<StreamContext,Boolean> consumer){
        Validate.notBlank(attribute);
        return new EventMatcher(Predicates.attributes(attribute), consumer);
    }



    public static EventMatcher attributeValues(String[] attributeAndValues, CheckedFunction<StreamContext,Boolean> consumer){
        Validate.notEmpty(attributeAndValues);
        return new EventMatcher(Predicates.attributeValue(attributeAndValues), consumer);
    }

    public static EventMatcher attributeValue(String attribute, String value, CheckedFunction<StreamContext,Boolean> consumer){
        Validate.notBlank(attribute);
        return new EventMatcher(new AttributeValuesPredicate(true, false, attribute, value), consumer);
    }

}
