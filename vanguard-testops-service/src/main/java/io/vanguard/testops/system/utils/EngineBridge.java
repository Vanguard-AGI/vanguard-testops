package io.vanguard.testops.system.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.metersphere.engine.EngineFactory;
import io.metersphere.engine.MsHttpClient;
import io.vanguard.testops.sdk.dto.api.task.TaskBatchRequestDTO;
import io.vanguard.testops.sdk.dto.api.task.TaskRequestDTO;
import io.vanguard.testops.system.dto.pool.TestResourceDTO;

import java.lang.reflect.Method;
import java.util.Collection;

public final class EngineBridge {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Class<?> ENGINE_TASK_REQUEST_CLASS = loadClass("io.metersphere.sdk.dto.api.task.TaskRequestDTO");
    private static final Class<?> ENGINE_TASK_BATCH_REQUEST_CLASS = loadClass("io.metersphere.sdk.dto.api.task.TaskBatchRequestDTO");
    private static final Class<?> ENGINE_TEST_RESOURCE_CLASS = loadClass("io.metersphere.system.dto.pool.TestResourceDTO");

    private EngineBridge() {
    }

    public static void debugApi(String endpoint, TaskRequestDTO taskRequest) throws Exception {
        invoke(MsHttpClient.class, "debugApi",
                new Class<?>[]{String.class, ENGINE_TASK_REQUEST_CLASS},
                endpoint, convert(taskRequest, ENGINE_TASK_REQUEST_CLASS));
    }

    public static void runApi(String endpoint, TaskRequestDTO taskRequest) throws Exception {
        invoke(MsHttpClient.class, "runApi",
                new Class<?>[]{String.class, ENGINE_TASK_REQUEST_CLASS},
                endpoint, convert(taskRequest, ENGINE_TASK_REQUEST_CLASS));
    }

    public static void batchRunApi(String endpoint, TaskBatchRequestDTO taskRequest) throws Exception {
        invoke(MsHttpClient.class, "batchRunApi",
                new Class<?>[]{String.class, ENGINE_TASK_BATCH_REQUEST_CLASS},
                endpoint, convert(taskRequest, ENGINE_TASK_BATCH_REQUEST_CLASS));
    }

    public static void debugApi(TaskRequestDTO taskRequest, TestResourceDTO testResourceDTO) throws Exception {
        invoke(EngineFactory.class, "debugApi",
                new Class<?>[]{ENGINE_TASK_REQUEST_CLASS, ENGINE_TEST_RESOURCE_CLASS},
                convert(taskRequest, ENGINE_TASK_REQUEST_CLASS),
                convert(testResourceDTO, ENGINE_TEST_RESOURCE_CLASS));
    }

    public static void runApi(TaskRequestDTO taskRequest, TestResourceDTO testResourceDTO) throws Exception {
        invoke(EngineFactory.class, "runApi",
                new Class<?>[]{ENGINE_TASK_REQUEST_CLASS, ENGINE_TEST_RESOURCE_CLASS},
                convert(taskRequest, ENGINE_TASK_REQUEST_CLASS),
                convert(testResourceDTO, ENGINE_TEST_RESOURCE_CLASS));
    }

    public static void batchRunApi(TaskBatchRequestDTO taskRequest, TestResourceDTO testResourceDTO) throws Exception {
        invoke(EngineFactory.class, "batchRunApi",
                new Class<?>[]{ENGINE_TASK_BATCH_REQUEST_CLASS, ENGINE_TEST_RESOURCE_CLASS},
                convert(taskRequest, ENGINE_TASK_BATCH_REQUEST_CLASS),
                convert(testResourceDTO, ENGINE_TEST_RESOURCE_CLASS));
    }

    public static void stopApiTask(Collection<String> ids, TestResourceDTO testResourceDTO) throws Exception {
        invoke(EngineFactory.class, "stopApiTask",
                new Class<?>[]{Collection.class, ENGINE_TEST_RESOURCE_CLASS},
                ids, convert(testResourceDTO, ENGINE_TEST_RESOURCE_CLASS));
    }

    public static void stopApiTaskItem(Collection<String> ids, TestResourceDTO testResourceDTO) throws Exception {
        invoke(EngineFactory.class, "stopApiTaskItem",
                new Class<?>[]{Collection.class, ENGINE_TEST_RESOURCE_CLASS},
                ids, convert(testResourceDTO, ENGINE_TEST_RESOURCE_CLASS));
    }

    public static boolean validateNamespaceExists(TestResourceDTO testResourceDTO) {
        try {
            return (Boolean) invoke(EngineFactory.class, "validateNamespaceExists",
                    new Class<?>[]{ENGINE_TEST_RESOURCE_CLASS},
                    convert(testResourceDTO, ENGINE_TEST_RESOURCE_CLASS));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate namespace", e);
        }
    }

    private static Object convert(Object source, Class<?> targetClass) {
        return OBJECT_MAPPER.convertValue(source, targetClass);
    }

    private static Object invoke(Class<?> targetClass, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = targetClass.getMethod(methodName, parameterTypes);
        return method.invoke(null, args);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing engine compatibility class: " + className, e);
        }
    }
}
