package com.klix.backend.validators;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.klix.backend.validators.interfaces.UniqueConstraint;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;

import lombok.extern.slf4j.Slf4j;


/**
 * 
 */
@Slf4j
public class UniqueValidator implements ConstraintValidator<UniqueConstraint, Object>
{
    @Autowired
    public BeanFactory beanFactory;

    private CrudRepository<?, ?> repo;

    private String field;


    /**
     * 
     */
    @Override
    public void initialize(UniqueConstraint constraintAnnotation)
    {
        this.field = constraintAnnotation.field();
        this.repo = beanFactory != null ? beanFactory.getBean(constraintAnnotation.repo()) : null;      // is only null during general initialization, ignore
    }


    /**
     * 
     */
    @Override
    public boolean isValid(Object model, ConstraintValidatorContext context)
    {
        // only happens during general initialization, ignore
        if (repo == null) return true;

        BeanWrapper modelWrapper = new BeanWrapperImpl(model);

        // get the value
        Object value = modelWrapper.getPropertyValue(field);
        if (value == null) return false;

        // get repo function to check for duplicates
        String upperField = field.substring(0, 1).toUpperCase() + field.substring(1);
        Method lookup = getMethod(repo.getClass(), "findBy" + upperField, value.getClass());
        if (lookup == null) return false;

        Class<?> returnType = lookup.getReturnType();
        
        Object oresult = returnType.cast(invoke(repo, lookup, value));
        Object result;
        if (oresult instanceof Optional<?>) {
            result = ((Optional<?>)oresult).orElse(null);
        }
        else{
            result = oresult;
        }

        if (result == null) return true;

        if (result instanceof Collection<?>) {
            if ( ((Collection<?>)result).isEmpty()) return true;

            return modelWrapper.getPropertyValue("id") == new BeanWrapperImpl( ((Collection<?>)result).toArray()[0]).getPropertyValue("id");
        }

        return modelWrapper.getPropertyValue("id") == new BeanWrapperImpl(result).getPropertyValue("id");

        // if not empty, check if the id is the same -> no duplicate
        //assert(result.size() == 1);
        //if (result instanceof Collection<?>) {
        

        
            //Collection<?> result =((Collection<?>) invoke(repo, lookup, value));
            //if (result == null) return false;


            // Object something = "something";
            // String theType = "java.lang.String";
            // Class<?> theClass = Class.forName(theType);
            // Object obj = theClass.cast(something);
        
            // Collection<?> result =((Collection<?>) invoke(repo, lookup, value));
            // if (result == null) return false;
        

        // no duplicate if none found
        //if (result.isEmpty()) return true;


        // // if not empty, check if the id is the same -> no duplicate
        // assert(result.size() == 1);

        // return modelWrapper.getPropertyValue("id") == new BeanWrapperImpl(result.toArray()[0]).getPropertyValue("id");
    }


    /**
     * 
     */
    private Method getMethod(Class<?> instanceClass, String name, Class<?>... paramClasses)
    {
        Method function;
        try {
            function = instanceClass.getMethod(name, paramClasses);
        } catch (NoSuchMethodException | SecurityException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
        return function;
    }


    /**
     * 
     */
    private Object invoke(Object instance, Method method, Object... params)
    {
        Object value;
        try {
            value = method.invoke(instance, params);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
        return value;
    }
}
