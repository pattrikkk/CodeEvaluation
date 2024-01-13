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
        Class<?> mySolution = MySolution.class;
        Class<?> testClass = TestClass.class;


        List<Method>[] methods = findSameMethods(mySolution, testClass);
        List<Method> sameMethods = methods[0];
        List<Method> missingMethods = methods[1];

        JSONObject config = getDataFromJson();
        JSONObject tests = config.getJSONObject("testcases");
        String[] testNames = JSONObject.getNames(tests);

        for (String name : testNames) {
            System.out.println(name + ":");
            runTest(sameMethods, config, name);
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

    public static void runTest(List<Method> sameMethods, JSONObject config, String testName) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        JSONObject configObject = config.getJSONObject("testcases");
        JSONArray parameters = configObject.optJSONObject(testName).optJSONArray("constructor");
        Object[] arguments = generateParameters(parameters);

        Class<?>[] argumentTypes = new Class[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentTypes[i] = arguments[i].getClass();
        }

        argumentTypes = changeType(argumentTypes);

        Constructor<?> constructor = checkIfConstructorExists(MySolution.class, argumentTypes);
        Constructor<?> testConstructor = checkIfConstructorExists(TestClass.class, argumentTypes);

        if (constructor == null || testConstructor == null) {
            System.out.println("Constructor(" + Arrays.toString(argumentTypes) + "): (failed)");
            return;
        }

        MySolution mySolution = (MySolution) constructor.newInstance(arguments);
        TestClass testClass = (TestClass) testConstructor.newInstance(arguments);

        System.out.println("Constructor(" + Arrays.toString(arguments) + "): (passed)");

        JSONArray methodsToRun = configObject.optJSONObject(testName).optJSONArray("methods");

        if (methodsToRun == null) {
            methodsToRun = sameMethods.stream()
                    .filter(method -> method.getParameterCount() == 0)
                    .map(method -> new JSONObject().put("method", method.getName()))
                    .reduce(new JSONArray(), JSONArray::put, JSONArray::put);
        }

        for (int i = 0; i < methodsToRun.length(); i++) {
            String methodName = methodsToRun.getJSONObject(i).getString("method");
            JSONArray methodParameters = methodsToRun.getJSONObject(i).optJSONArray("parameters");
            Object[] methodArguments;

            if (methodParameters == null) methodArguments = new Object[0];
            else methodArguments = generateParameters(methodParameters);

            Method method = sameMethods.stream()
                    .filter(m -> m.getName().equals(methodName))
                    .findFirst()
                    .orElse(null);
            if (method == null) {
                System.out.println("Method " + methodName + ": (Not found)");
                continue;
            }
            Method testMethod = TestClass.class.getDeclaredMethod(method.getName(), method.getParameterTypes());

            Object mySolutionResult = method.invoke(mySolution, methodArguments);
            Object testResult = testMethod.invoke(testClass, methodArguments);

            if (mySolutionResult == null) {
                System.out.println("Method " + method.getName() + " with values " + Arrays.toString(methodArguments) + ": (passed)");
                continue;
            }
            if (mySolutionResult.equals(testResult)) {
                System.out.println("✔ Method " + method.getName() + ": (returned " + mySolutionResult + ") (passed)");
            } else {
                System.out.println("X Method " + method.getName() + ": (returned " + testResult + " expected " + mySolutionResult + ") (failed)");
            }
        }
    }

    public static Constructor<?> checkIfConstructorExists(Class<?> classA, Class<?>[] argumentTypes) {
        try {
            return classA.getConstructor(argumentTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

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

    public static List<Method>[] findSameMethods(Class<?> mySolution, Class<?> testClass) {
        Method[] methodsA = mySolution.getDeclaredMethods();
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

    public static boolean areMethodsSignatureEqual(Method methodA, Method methodB) {
        return Objects.equals(methodA.getName(), methodB.getName()) &&
                Objects.equals(methodA.getReturnType(), methodB.getReturnType()) &&
                Arrays.equals(methodA.getParameterTypes(), methodB.getParameterTypes());
    }
}
