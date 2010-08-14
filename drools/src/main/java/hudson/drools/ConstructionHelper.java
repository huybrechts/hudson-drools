package hudson.drools;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

public class ConstructionHelper {

	public static <T> T newInstance(Class<T> type, Map<String, Object> parameters) {
		
		Constructor<?>[] constructors = type.getConstructors();
		if (constructors.length != 1) {
			throw new IllegalArgumentException("type " + type.getName() + " must have exactly one public constructor");
		}
		
		Constructor<T> constructor = (Constructor<T>) constructors[0];
		String[] names = new BytecodeReadingParanamer()
				.lookupParameterNames(constructor);

		// the actual arguments to invoke the constructor with.
		Object[] args = new Object[names.length];
		for (int i = 0; i < names.length; i++) {
			args[i] = parameters.get(names[i]);
		}

		return invokeConstructor(constructor, args);
	}

	private static <T> T invokeConstructor(Constructor<T> c, Object[] args) {
		try {
			return c.newInstance(args);
		} catch (InstantiationException e) {
			InstantiationError x = new InstantiationError(e.getMessage());
			x.initCause(e);
			throw x;
		} catch (IllegalAccessException e) {
			IllegalAccessError x = new IllegalAccessError(e.getMessage());
			x.initCause(e);
			throw x;
		} catch (InvocationTargetException e) {
			Throwable x = e.getTargetException();
			if (x instanceof Error)
				throw (Error) x;
			if (x instanceof RuntimeException)
				throw (RuntimeException) x;
			throw new IllegalArgumentException(x);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed to invoke " + c
					+ " with " + Arrays.asList(args), e);
		}
	}

}
