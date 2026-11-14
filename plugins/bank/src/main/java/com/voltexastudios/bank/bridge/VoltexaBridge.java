package com.voltexastudios.bank.bridge;

import com.voltexastudios.voltexa.api.ConfigService;
import com.voltexastudios.voltexa.api.DatabaseService;
import com.voltexastudios.voltexa.api.LogService;
import com.voltexastudios.voltexa.api.MigrationService;
import com.voltexastudios.voltexa.api.ThrowingFunction;
import com.voltexastudios.voltexa.api.VoltexaAPI;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicesManager;

/**
 * Classloader bridge between Bank and the Voltexa core.
 *
 * <p>Both plugins bundle the same {@code voltexa-api} contract, but Bukkit
 * isolates each plugin's classloader, so Bank's copy of the contract types is
 * a different {@link Class} object from the core's. The Bukkit
 * {@code ServicesManager} matches registrations by {@code Class} identity, and
 * contract data carriers (e.g. {@code MigrationRecord}) cannot be cast across
 * classloaders, so Bank cannot use its own copy of the contract directly.
 *
 * <p>This bridge resolves the core's service through the core plugin's own
 * classloader — so the {@code ServicesManager} lookup sees the same
 * {@code Class} object the core registered — and wraps the result in a dynamic
 * proxy that implements Bank's copy of the contract. Bank's typed code then
 * works unchanged, with no JVM flags and no shared classpath entry.
 *
 * <p>Contract types that cross the boundary are translated: functional
 * arguments (Bank's {@code ThrowingFunction} lambdas) are wrapped for the
 * core, and record data carriers coming back from the core are
 * re-materialized as Bank's copies. Everything else (JDK and Paper API types,
 * Bank's own types) passes through as-is.
 *
 * <p>Nothing here is on a hot path: the proxies are created once in
 * {@code onEnable} and then used as ordinary typed objects.
 */
public final class VoltexaBridge {

    /** Contract package prefix; data carriers in it are translated per element. */
    private static final String API_PACKAGE = "com.voltexastudios.voltexa.api.";

    /** The contract types translated across the boundary (Bank's copies). */
    private static final Class<?>[] CONTRACT_TYPES = {
        VoltexaAPI.class, DatabaseService.class, ConfigService.class,
        LogService.class, MigrationService.class, ThrowingFunction.class
    };

    /** Simple names of the contract types (for core-side detection by name). */
    private static final java.util.Set<String> CONTRACT_SIMPLE_NAMES =
            java.util.Set.of("VoltexaAPI", "DatabaseService", "ConfigService",
                    "LogService", "MigrationService", "ThrowingFunction");

    private final ClassLoader coreClassLoader;
    /** Remote (core-side) contract classes, resolved by name, cached. */
    private final Map<String, Class<?>> remoteContractCache = new ConcurrentHashMap<>();

    private VoltexaBridge(ClassLoader coreClassLoader) {
        this.coreClassLoader = coreClassLoader;
    }

    /**
     * Resolves the core's {@code VoltexaAPI} service and wraps it for Bank.
     *
     * @param bank the Bank plugin (its classloader loads Bank's contract copy)
     * @return the wrapped API, or {@code null} when the core plugin is missing,
     *         disabled, or does not register the service
     */
    public static VoltexaAPI resolve(Plugin bank) {
        Plugin core = bank.getServer().getPluginManager().getPlugin("Voltexa");
        if (core == null || !core.isEnabled()) {
            return null;
        }
        // The core's main class is loaded by the core's plugin classloader;
        // resolving the contract through it yields the same Class objects the
        // core used to register its services.
        ClassLoader coreCl = core.getClass().getClassLoader();
        try {
            Class<?> remoteApi = Class.forName(API_PACKAGE + "VoltexaAPI", false, coreCl);
            ServicesManager services = bank.getServer().getServicesManager();
            Object raw = services.load(remoteApi);
            if (raw == null) {
                return null;
            }
            return new VoltexaBridge(coreCl).wrap(raw, VoltexaAPI.class);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Wraps a core-side object for Bank's copy of the contract. Exposed for
     * the unit test, which simulates the two-classloader setup.
     */
    static VoltexaAPI wrap(ClassLoader coreClassLoader, Object raw) {
        return new VoltexaBridge(coreClassLoader).wrap(raw, VoltexaAPI.class);
    }

    /**
     * Wraps a core-side object implementing the given local contract type. The
     * proxy is defined in Bank's classloader and implements Bank's copy of
     * the contract, so Bank's typed code sees a normal typed object.
     */
    @SuppressWarnings("unchecked")
    <T> T wrap(Object raw, Class<T> localType) {
        return (T) Proxy.newProxyInstance(
                VoltexaBridge.class.getClassLoader(),
                new Class<?>[] {localType},
                new ToCoreHandler(raw, localType));
    }

    /** The core-side contract class for a local contract type (by name). */
    private Class<?> remoteContract(Class<?> localType) {
        return remoteContractCache.computeIfAbsent(localType.getName(), name -> {
            try {
                return Class.forName(name, false, coreClassLoader);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "core classloader is missing contract type " + name, e);
            }
        });
    }

    /**
     * True when the type (either side's copy) is one of the contract types.
     * Detection is by name: both classloaders load the same fully-qualified
     * names, so this works for local and core-side classes alike.
     */
    private boolean isContractType(Class<?> type) {
        String name = type.getName();
        int dot = name.lastIndexOf('.');
        return name.startsWith(API_PACKAGE)
                && CONTRACT_SIMPLE_NAMES.contains(name.substring(dot + 1));
    }

    /**
     * Forwards calls on a Bank-side proxy to the wrapped core-side object,
     * translating contract types in both directions.
     */
    private final class ToCoreHandler implements InvocationHandler {
        private final Object target;
        private final Class<?> localType;

        ToCoreHandler(Object target, Class<?> localType) {
            this.target = target;
            this.localType = localType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Object methods on the proxy itself (equals/hashCode/toString).
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "VoltexaBridge$" + localType.getSimpleName();
                    case "hashCode" -> System.identityHashCode(target);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            // The JVM passes a null args array for zero-argument methods.
            Object[] forwarded = new Object[args == null ? 0 : args.length];
            for (int i = 0; i < forwarded.length; i++) {
                forwarded[i] = wrapForCore(args[i]);
            }
            try {
                return translateResult(
                        remoteMethod(target, method).invoke(target, forwarded));
            } catch (InvocationTargetException e) {
                // Re-throw the underlying exception, not the reflection wrapper.
                throw e.getCause() == null ? e : e.getCause();
            }
        }
    }

    /** Resolves the core-side method for a local contract method. */
    private Method remoteMethod(Object target, Method localMethod)
            throws NoSuchMethodException {
        Class<?>[] localParams = localMethod.getParameterTypes();
        Class<?>[] remoteParams = new Class<?>[localParams.length];
        for (int i = 0; i < localParams.length; i++) {
            remoteParams[i] = isContractType(localParams[i])
                    ? remoteContract(localParams[i])
                    : localParams[i];
        }
        return target.getClass().getMethod(localMethod.getName(), remoteParams);
    }

    /**
     * Wraps a Bank-side contract instance (e.g. a {@code ThrowingFunction}
     * lambda) in a proxy of the core-side contract type, calling back into
     * the Bank-side object. Non-contract arguments pass through.
     */
    private Object wrapForCore(Object arg) {
        if (arg == null) {
            return null;
        }
        Class<?> contract = contractOf(arg);
        if (contract != null) {
            return Proxy.newProxyInstance(coreClassLoader,
                    new Class<?>[] {remoteContract(contract)},
                    new ToBankHandler(arg, contract));
        }
        // Record data carriers (Bank-side copies) are re-materialized as
        // core-side copies; the core cannot see Bank's copies.
        Class<?> argClass = arg.getClass();
        if (argClass.isRecord() && argClass.getName().startsWith(API_PACKAGE)) {
            return reMaterialize(arg, argClass, remoteContract(argClass));
        }
        if (arg instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            Class<?> elementClass = first.getClass();
            if (elementClass.isRecord()
                    && elementClass.getName().startsWith(API_PACKAGE)) {
                Class<?> coreRecord = remoteContract(elementClass);
                List<Object> translated = new ArrayList<>(list.size());
                for (Object element : list) {
                    translated.add(reMaterialize(element, elementClass, coreRecord));
                }
                return translated;
            }
        }
        return arg;
    }

    /** The contract type an object implements, or null. */
    private Class<?> contractOf(Object arg) {
        for (Class<?> type : CONTRACT_TYPES) {
            if (type.isInstance(arg)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Translates a result coming from the core back to Bank's types: contract
     * instances become Bank-side proxies, lists of core-side contract records
     * become lists of Bank-side records, everything else passes through.
     */
    private Object translateResult(Object result) {
        if (result == null) {
            return null;
        }
        // The result is a core-side object (a concrete implementation or a
        // proxy), so its class name is not the contract's: detect contract
        // types by isInstance against the core-side copies, which the
        // core-side object actually implements.
        for (Class<?> localType : CONTRACT_TYPES) {
            if (remoteContract(localType).isInstance(result)) {
                return wrap(result, localType);
            }
        }
        Class<?> resultClass = result.getClass();
        if (resultClass.isRecord()
                && resultClass.getName().startsWith(API_PACKAGE)) {
            return reMaterialize(result, resultClass, localCopy(resultClass));
        }
        if (result instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            Class<?> elementClass = first.getClass();
            if (elementClass.isRecord()
                    && elementClass.getName().startsWith(API_PACKAGE)) {
                Class<?> localRecord = localCopy(elementClass);
                List<Object> translated = new ArrayList<>(list.size());
                for (Object element : list) {
                    translated.add(reMaterialize(element, elementClass, localRecord));
                }
                return translated;
            }
        }
        return result;
    }

    /** Bank's copy of a core-side contract type (by name). */
    private Class<?> localCopy(Class<?> coreType) {
        try {
            return Class.forName(coreType.getName(), false,
                    VoltexaBridge.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "bank classloader is missing contract type "
                            + coreType.getName(), e);
        }
    }

    /**
     * Re-materializes a record from one side's classloader as the other
     * side's copy by reading each record component and invoking the target's
     * canonical constructor. Works in either direction; component types must
     * be shared JDK types (they are, for the current contract records).
     */
    private Object reMaterialize(Object sourceRecord, Class<?> sourceClass,
                                Class<?> targetClass) {
        try {
            RecordComponent[] components = sourceClass.getRecordComponents();
            RecordComponent[] targetComponents = targetClass.getRecordComponents();
            Object[] values = new Object[components.length];
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                values[i] = sourceClass
                        .getMethod(components[i].getName()).invoke(sourceRecord);
                paramTypes[i] = targetComponents[i].getType();
            }
            Constructor<?> constructor =
                    targetClass.getDeclaredConstructor(paramTypes);
            return constructor.newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "cannot translate record " + sourceClass.getName()
                            + " -> " + targetClass.getName(), e);
        }
    }

    /**
     * Calls back from a core-side proxy (e.g. the core invoking a
     * {@code ThrowingFunction} lambda) into the Bank-side object. Arguments
     * and results are shared types (JDK/Paper) and pass through.
     */
    private final class ToBankHandler implements InvocationHandler {
        private final Object bankSide;
        private final Class<?> localContract;

        ToBankHandler(Object bankSide, Class<?> localContract) {
            this.bankSide = bankSide;
            this.localContract = localContract;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Object methods on the proxy itself (equals/hashCode/toString).
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "VoltexaBridge$back$" + localContract.getSimpleName();
                    case "hashCode" -> System.identityHashCode(bankSide);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            try {
                Method localMethod = localContract.getMethod(
                        method.getName(), localParamsFor(method));
                return localMethod.invoke(bankSide, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        /** The Bank-side parameter types for a core-side method. */
        private Class<?>[] localParamsFor(Method coreMethod) {
            Class<?>[] coreParams = coreMethod.getParameterTypes();
            Class<?>[] localParams = new Class<?>[coreParams.length];
            for (int i = 0; i < coreParams.length; i++) {
                localParams[i] = isContractType(coreParams[i])
                        ? localCopy(coreParams[i])
                        : coreParams[i];
            }
            return localParams;
        }
    }
}
