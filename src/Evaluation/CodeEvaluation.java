package Evaluation;

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
    public static int successfulTests = 0;
    public static int allTests = 0;
    public static int executedMethods = 0;
    public static int successfulMethods = 0;
    public static void main(String[] args) {
        try {
            //CodeEvaluation.setup(Zadanie1.ReferenceClass.class, Zadanie1.Spider.class, "src/" + Zadanie1.ReferenceClass.class.getPackageName() + "/config.json");
            //CodeEvaluation.setup(Zadanie2.ReferenceClass.class, Zadanie2.Label.class, "src/" + Zadanie2.ReferenceClass.class.getPackageName() + "/config.json");
            CodeEvaluation.setup(Zadanie3.ReferenceClass.class, Zadanie3.Bus.class, "src/" + Zadanie3.ReferenceClass.class.getPackageName() + "/config.json");
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    public static void setup(Class<?> referenceClass, Class<?> testClass, String configPath) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        List<Method>[] methods = findSameMethods(referenceClass, testClass);
        List<Method> sameMethods = methods[0];
        List<Method> missingMethods = methods[1];

        JSONObject config = getDataFromJson(configPath);
        JSONObject tests = config.getJSONObject("testcases");
        String[] testNames = JSONObject.getNames(tests);

        for (String name : testNames) {
            System.out.println(name + ":");
            allTests++;
            runTest(sameMethods, name, referenceClass, testClass, config);
            if (successfulMethods == executedMethods) successfulTests++;
            System.out.println("Successful/Executed methods: " + successfulMethods + "/" + executedMethods + " " + (100.0 / executedMethods) * successfulMethods + "%\n");
            successfulMethods = 0;
            executedMethods = 0;
        }

        if (missingMethods.size() > 0) {
            System.out.println("Missing methods:");
            for (Method method : missingMethods) {
                String parameters = Arrays.toString(method.getParameterTypes());
                System.out.println(method.getReturnType() + " " + method.getName() + "(" + parameters.substring(1, parameters.length()-1) + ")");
            }
        }
        System.out.println("\nSuccessful/Executed tests: " + successfulTests + "/" + allTests + " " + 100.0/allTests*successfulTests + "%");
    }

    public static void runTest(List<Method> sameMethods, String testName, Class<?> referenceClass, Class<?> testClass, JSONObject config) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        JSONObject configObject = config.getJSONObject("testcases");
        JSONArray parameters = configObject.optJSONObject(testName).optJSONArray("constructor");
        Object[] arguments = generateParameters(parameters, config);

        Class<?>[] argumentTypes = new Class[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentTypes[i] = arguments[i].getClass();
        }

        argumentTypes = changeType(argumentTypes);

        Constructor<?> constructor = checkIfConstructorExists(referenceClass, argumentTypes);
        Constructor<?> testConstructor = checkIfConstructorExists(testClass, argumentTypes);

        StringBuilder constructorArguments = new StringBuilder();
        for (Object argument : arguments) {
            if (argument.getClass().isArray()) {
                constructorArguments.append(Arrays.toString((int[]) argument)).append(", ");
            } else constructorArguments.append(argument.toString()).append(", ");
        }

        constructorArguments = new StringBuilder(constructorArguments.substring(0, constructorArguments.length() - 2));

        if (constructor == null || testConstructor == null) {
            System.out.println("Constructor(" + constructorArguments + "): (failed)");
            return;
        }

        constructor.setAccessible(true);
        testConstructor.setAccessible(true);

        Object referenceInstance = null;
        Object testInstance = null;
        String message = "";
        String testMessage = "";
        try {
            referenceInstance = constructor.newInstance(arguments);
        } catch (Exception e) {
            message = e.getCause().toString();
        }
        try {
            testInstance = testConstructor.newInstance(arguments);
        } catch (Exception e) {
            testMessage = e.getCause().toString();
        }

        if (!message.equals(testMessage)) {
            System.out.println("Constructor(" + constructorArguments + "): (" + testMessage + " expected " + message + ") (failed)");
            return;
        }

        if (!message.equals("")) {
            System.out.println("Constructor(" + constructorArguments + "): (" + message + ") (passed)");
            return;
        }


        System.out.println("Constructor(" + constructorArguments + "): (passed)");

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

            executedMethods++;

            if (methodParameters == null) methodArguments = new Object[0];
            else methodArguments = generateParameters(methodParameters, config);

            Method method = sameMethods.stream()
                    .filter(m -> m.getName().equals(methodName))
                    .findFirst()
                    .orElse(null);
            if (method == null) {
                System.out.println("Method " + methodName + "(): (Not found)");
                continue;
            }
            Method testMethod = testClass.getDeclaredMethod(method.getName(), method.getParameterTypes());

            if (method.getParameterCount() != methodArguments.length) {
                System.out.println("Method " + method.getReturnType() + " " + method.getName() + "(" + Arrays.toString(method.getParameterTypes()).substring(1, Arrays.toString(method.getParameterTypes()).length() - 1) + "): (Wrong amount of arguments - error in reference class or config)");
                continue;
            }


            Class<?>[] methodArgumentTypes = new Class[methodArguments.length];
            for (int j = 0; j < methodArguments.length; j++) {
                methodArgumentTypes[j] = methodArguments[j].getClass();
            }
            methodArgumentTypes = changeType(methodArgumentTypes);

            boolean argumentTypeMismatch = false;
            for (int j = 0; j < methodArguments.length; j++) {
                if (!method.getParameterTypes()[j].equals(methodArgumentTypes[j])) {
                    argumentTypeMismatch = true;
                    System.out.println("Method " + method.getReturnType() + " " + method.getName() + "(" + Arrays.toString(method.getParameterTypes()).substring(1, Arrays.toString(method.getParameterTypes()).length() - 1) + "): (Argument type mismatch - error in reference class or config)");
                }
            }
            if (argumentTypeMismatch) continue;

            method.setAccessible(true);
            testMethod.setAccessible(true);

            Object referenceResult;
            Object testResult;

            try {
                referenceResult = method.invoke(referenceInstance, methodArguments);
            } catch (Exception e) {
                referenceResult = e.getCause().toString();
            }
            try {
                testResult = testMethod.invoke(testInstance, methodArguments);
            } catch (Exception e) {
                testResult = e.getCause().toString();
            }


            String argumentString = Arrays.toString(methodArguments).substring(1, Arrays.toString(methodArguments).length() - 1);

            if (referenceResult == null || referenceResult.equals(testResult)) {
                successfulMethods++;
                System.out.println("✔ Method " + method.getReturnType() + " " + method.getName() + "(" + argumentString + ")" + ": (returned " + referenceResult + ") (passed)");
            } else {
                System.out.println("X Method " + method.getReturnType() + " " + method.getName() + "(" + argumentString + ")" + ": (returned " + testResult + " expected " + referenceResult + ") (failed)");
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

    public static Object[] generateParameters(JSONArray parameters, JSONObject config) {
        Random random = new Random();
        Object[] arguments = new Object[parameters.length()];
        for (int i = 0; i < parameters.length(); i++) {
            if (parameters.getString(i).charAt(0) == 'i') {
                arguments[i] = getIntValue(parameters.getString(i).substring(1));
            } else if (parameters.getString(i).charAt(0) == 's') {
                String testedString = parameters.getString(i).substring(1);
                arguments[i] = testedString;
            } else if (parameters.getString(i).charAt(0) == 'c') {
                char testedChar = parameters.getString(i).charAt(1);
                arguments[i] = testedChar;
            } else {
                String a = parameters.getString(i);
                JSONArray stringArray = config.optJSONArray(a.substring(1));
                String test = stringArray.getString(random.nextInt(stringArray.length()));
                arguments[i] = test;
            }
        }
        return arguments;
    }

    public static JSONObject getDataFromJson(String path) {
        try (FileReader fileReader = new FileReader(path)) {
            JSONTokener tokenizer = new JSONTokener(fileReader);
            return new JSONObject(tokenizer);
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public static int getIntValue(String input) {
        char t = input.charAt(0);
        int maxNumericValue = Integer.parseInt(input.substring(1).split(",")[0]);
        double val = 0;
        switch (t) {
            case 'P' -> val = 1 + (Math.random() * maxNumericValue);
            case 'N' -> val = -(Math.random() * maxNumericValue);
            case 'Z' -> val = 0;
            case 'R' -> val = -maxNumericValue + (Math.random() * (2 * maxNumericValue + 1));
            case 'X' -> val = Double.parseDouble(input.substring(1).split(",")[0]);
            case 'I' -> {
                String[] interv = input.substring(1).split(",");
                val = Double.parseDouble(interv[0]) + (int) (Math.random() * (Double.parseDouble(interv[1]) - Double.parseDouble(interv[0]) + 1));
            }
        }
        return (int) val;
    }

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
            } else if (types[i].equals(Character.class)) {
                newTypes[i] = char.class;
            } else {
                newTypes[i] = types[i];
            }
        }
        return newTypes;
    }

    public static boolean areMethodsSignatureEqual(Method methodA, Method methodB) {
        return Objects.equals(methodA.getName(), methodB.getName()) &&
                Objects.equals(methodA.getReturnType(), methodB.getReturnType()) &&
                Arrays.equals(methodA.getParameterTypes(), methodB.getParameterTypes());
    }
}
