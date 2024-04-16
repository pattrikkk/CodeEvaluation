import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CodeEvaluation {
    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Class<?> referenceClass = ReferenceClass.class;
        Class<?> testClass = TestClass.class;


        List<Method>[] methods = findSameMethods(referenceClass, testClass);
        List<Method> sameMethods = methods[0];
        List<Method> missingMethods = methods[1];

        JSONObject config = getDataFromJson();
        JSONObject tests = config.getJSONObject("testcases");
        String[] testNames = JSONObject.getNames(tests);

        for (String name : testNames) {
            System.out.println(name + ":");
            runTest(sameMethods, name);
            System.out.println();
        }

        if (missingMethods.size() > 0) {
            System.out.println("Missing methods:");
            for (Method method : missingMethods) {
                String parameters = Arrays.toString(method.getParameterTypes());
                System.out.println(method.getReturnType() + " " + method.getName() + "(" + parameters.substring(1, parameters.length()-1) + ")");
            }
        }
    }

    public static void runTest(List<Method> sameMethods, String testName) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        JSONObject config = getDataFromJson();
        JSONObject configObject = config.getJSONObject("testcases");
        JSONArray parameters = configObject.optJSONObject(testName).optJSONArray("constructor");
        Object[] arguments = generateParameters(parameters);

        Class<?>[] argumentTypes = new Class[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentTypes[i] = arguments[i].getClass();
        }

        argumentTypes = changeType(argumentTypes);

        Constructor<?> constructor = checkIfConstructorExists(ReferenceClass.class, argumentTypes);
        Constructor<?> testConstructor = checkIfConstructorExists(TestClass.class, argumentTypes);

        String constructorArguments = "";
        for (Object argument : arguments) {
            if (argument.getClass().isArray()) {
                constructorArguments += Arrays.toString((int[]) argument) + ", ";
            } else constructorArguments += argument.toString() + ", ";
        }

        constructorArguments = constructorArguments.substring(0, constructorArguments.length() - 2);

        if (constructor == null || testConstructor == null) {
            System.out.println("Constructor(" + constructorArguments + "): (failed)");
            return;
        }

        System.out.println("Constructor(" + constructorArguments + "): (passed)");

        ReferenceClass referenceInstance = (ReferenceClass) constructor.newInstance(arguments);
        TestClass testInstance = (TestClass) testConstructor.newInstance(arguments);

        JSONArray methodsToRun = configObject.optJSONObject(testName).optJSONArray("methods");

        if (methodsToRun == null) {
            methodsToRun = sameMethods.stream()
                    .filter(method -> method.getParameterCount() == 0)
                    .map(method -> new JSONObject().put("method", method.getName()))
                    .reduce(new JSONArray(), JSONArray::put, JSONArray::put);
        }

        //Run methods from the JSON file
        for (int i = 0; i < methodsToRun.length(); i++) {
            String methodName = methodsToRun.getJSONObject(i).getString("method");
            JSONArray methodParameters = methodsToRun.getJSONObject(i).optJSONArray("parameters");
            Object[] methodArguments;

            //Generate the arguments for the method
            if (methodParameters == null) methodArguments = new Object[0];
            else methodArguments = generateParameters(methodParameters);

            //Find the method in the reference class
            Method method = sameMethods.stream()
                    .filter(m -> m.getName().equals(methodName))
                    .findFirst()
                    .orElse(null);
            if (method == null) {
                System.out.println("Method " + methodName + ": (Not found)");
                continue;
            }
            Method testMethod = TestClass.class.getDeclaredMethod(method.getName(), method.getParameterTypes());

            //Check if the argument count is correct
            if (method.getParameterCount() != methodArguments.length) {
                System.out.println("Method " + method.getReturnType() + " " + method.getName() + "(" + Arrays.toString(method.getParameterTypes()).substring(1, Arrays.toString(method.getParameterTypes()).length() - 1) + "): (Wrong amount of arguments)");
                continue;
            }


            Class<?>[] methodArgumentTypes = new Class[methodArguments.length];
            for (int j = 0; j < methodArguments.length; j++) {
                methodArgumentTypes[j] = methodArguments[j].getClass();
            }
            methodArgumentTypes = changeType(methodArgumentTypes);

            //Check for argument type mismatch
            boolean argumentTypeMismatch = false;
            for (int j = 0; j < methodArguments.length; j++) {
                if (!method.getParameterTypes()[j].equals(methodArgumentTypes[j])) {
                    argumentTypeMismatch = true;
                    System.out.println("Method " + method.getReturnType() + " " + method.getName() + "(" + Arrays.toString(method.getParameterTypes()).substring(1, Arrays.toString(method.getParameterTypes()).length() - 1) + "): (Argument type mismatch)");
                }
            }
            if (argumentTypeMismatch) continue;

            //Invoke the methods
            Object referenceResult = method.invoke(referenceInstance, methodArguments);
            Object testResult = testMethod.invoke(testInstance, methodArguments);

            //Create a string of argument types
            String argumentString = Arrays.toString(methodArguments).substring(1, Arrays.toString(methodArguments).length() - 1);

            //Print the results
            if (referenceResult == null || referenceResult.equals(testResult)) {
                System.out.println("✔ Method " + method.getReturnType() + " " + method.getName() + "(" + argumentString + ")" + ": (returned " + referenceResult + ") (passed)");
            } else {
                System.out.println("X Method " + method.getReturnType() + " " + method.getName() + "(" + argumentString + ")" + ": (returned " + testResult + " expected " + referenceResult + ") (failed)");
            }
        }
    }

    //Checks if a constructor exists in a class
    public static Constructor<?> checkIfConstructorExists(Class<?> classA, Class<?>[] argumentTypes) {
        try {
            return classA.getConstructor(argumentTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    //Generates the parameters for the constructor and methods
    public static Object[] generateParameters(JSONArray parameters) {
        Random random = new Random();
        Object[] arguments = new Object[parameters.length()];

        for (int i = 0; i < parameters.length(); i++) {
            if (parameters.getString(i).charAt(0) == 'i') {
                arguments[i] = getIntValue(parameters.getString(i).substring(1), 100);
            } else if (parameters.getString(i).charAt(0) == 'a') {
                String[] divided = parameters.getString(i).substring(2).split(",");
                int arrayLength = Integer.parseInt(divided[divided.length - 1]);
                int[] testedArray = new int[arrayLength];
                for (int j = 0; j < arrayLength; j++) {
                    testedArray[j] = getIntValue(parameters.getString(i).substring(1), 100);
                }
                arguments[i] = testedArray;
            } else if (parameters.getString(i).charAt(0) == 's') {
                String testedString = parameters.getString(i).substring(1);
                arguments[i] = testedString;
            } else {
                JSONObject config = getDataFromJson();
                String a = parameters.getString(i);
                JSONArray stringArray = config.optJSONArray(a.substring(1));
                String test = stringArray.getString(random.nextInt(stringArray.length()));
                arguments[i] = test;
            }
        }

        return arguments;
    }

    public static JSONObject getDataFromJson() {
        try (FileReader fileReader = new FileReader("src/config.json")) {
            JSONTokener tokenizer = new JSONTokener(fileReader);
            return new JSONObject(tokenizer);
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    //Generates a random integer based on the input
    public static int getIntValue(String input, int maxNumericValue) {
        char t = input.charAt(0);
        double val = 0;
        switch (t) {
            case 'P' -> val = 1 + (Math.random() * maxNumericValue);
            // positive
            case 'N' -> val = -(Math.random() * maxNumericValue);
            // negative
            case 'Z' -> val = 0;
            // zero
            case 'R' -> val = -maxNumericValue + (Math.random() * (2 * maxNumericValue + 1));
            // random
            case 'X' -> val = Double.parseDouble(input.substring(1).split(",")[0]);
            // exact number
            case 'I' -> {
                String[] interv = input.substring(1).split(",");
                val = Double.parseDouble(interv[0]) + (int) (Math.random() * (Double.parseDouble(interv[1]) - Double.parseDouble(interv[0]) + 1));
            } // interval
        }
        return (int) val;
    }

    //Static analysis of the classes to get the same and missing methods
    public static List<Method>[] findSameMethods(Class<?> referenceClass, Class<?> testClass) {
        Method[] methodsA = referenceClass.getDeclaredMethods();
        Method[] methodsB = testClass.getDeclaredMethods();

        List<Method> sameMethods = new ArrayList<>();
        List<Method> missingMethods = new ArrayList<>();

        for (Method methodA : methodsA) {
            Method matchingMethodB = Arrays.stream(methodsB)
                    .filter(method -> areMethodsSignatureEqual(methodA, method))
                    .findFirst()
                    .orElse(null);

            if (matchingMethodB != null) {
                sameMethods.add(methodA);
            } else {
                missingMethods.add(methodA);
            }
        }

        return new List[]{sameMethods, missingMethods};
    }

    public static Class<?>[] changeType(Class<?>[] types) {
        Class<?>[] newTypes = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(Integer.class)) {
                newTypes[i] = int.class;
            } else {
                newTypes[i] = types[i];
            }
        }
        return newTypes;
    }

    //Checks if two methods have the same name, return type and parameter types
    public static boolean areMethodsSignatureEqual(Method methodA, Method methodB) {
        return Objects.equals(methodA.getName(), methodB.getName()) &&
                Objects.equals(methodA.getReturnType(), methodB.getReturnType()) &&
                Arrays.equals(methodA.getParameterTypes(), methodB.getParameterTypes());
    }
}
