import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CodeEvaluation {
    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class<?> classA = MySolution.class;
        Class<?> classB = TestClass.class;

        List<Method>[] methods = findSameMethods(classA, classB);
        List<Method> sameMethods = methods[0];
        List<Method> missingMethods = methods[1];

        JSONObject config = getDataFromJson();
        JSONObject options = config.getJSONObject("options");

        int numberOfTests = options.getInt("numberOfTests");

        for (int i = 0; i < numberOfTests; i++) {
            System.out.println("Test " + (i + 1) + ":");
            runTest(sameMethods, config);
            System.out.println();
        }

        if (missingMethods.size() > 0) {
            System.out.println("Missing methods:");
            for (Method method : missingMethods) {
                System.out.println(method.getName());
            }
        }
    }

    public static void runTest(List<Method> sameMethods, JSONObject config) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        MySolution mySolution = new MySolution();
        TestClass testClass = new TestClass();
        Random random = new Random();
        for (Method method : sameMethods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];
            JSONArray configObject = config.getJSONArray("methods").getJSONObject(0).optJSONArray(method.getName());
            if (configObject == null) {
                configObject = config.getJSONArray("methods").getJSONObject(0).getJSONArray("default");
            }

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                String parameterTypeName = parameterType.getName();
                JSONArray parameterValue = configObject.getJSONObject(0).getJSONArray(parameterTypeName);

                int randomIndex = random.nextInt(parameterValue.length());
                arguments[i] = parameterValue.get(randomIndex);
            }
            Method testMethod = TestClass.class.getDeclaredMethod(method.getName(), parameterTypes);

            Object mySolutionResult = method.invoke(mySolution, arguments);
            Object testResult = testMethod.invoke(testClass, arguments);

            if (mySolutionResult == null) {
                continue;
            }
            if (mySolutionResult.equals(testResult)) {
                System.out.println("✔ Method " + method.getName() + ": " + mySolutionResult + " passed");
            } else {
                System.out.println("X Method " + method.getName() + " failed");
            }
        }
    }

    public static JSONObject getDataFromJson() {
        try (FileReader fileReader = new FileReader("src/config.json")) {
            JSONTokener tokener = new JSONTokener(fileReader);
            JSONObject jsonObject = new JSONObject(tokener);
            return jsonObject;
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public static List<Method>[] findSameMethods(Class<?> classA, Class<?> classB) {
        Method[] methodsA = classA.getDeclaredMethods();
        Method[] methodsB = classB.getDeclaredMethods();

        List<Method> sameMethods = new ArrayList<>();
        List<Method> missingMethods = new ArrayList<>();

        for (Method methodA : methodsA) {
            Method matchingMethodB = Arrays.stream(methodsB)
                    .filter(method -> areMethodsSignatureEqual(methodA, method))
                    .findFirst()
                    .orElse(null);

            if (matchingMethodB != null) {
                if (methodA.getReturnType().getName().equals("void")) sameMethods.add(0, methodA);
                else sameMethods.add(methodA);
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
