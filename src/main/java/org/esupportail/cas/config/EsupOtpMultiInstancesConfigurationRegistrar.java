package org.esupportail.cas.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.MultifactorAuthenticationFailureModeEvaluator;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.bypass.AuthenticationMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.ChainingMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.CredentialMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.DefaultChainingMultifactorAuthenticationBypassProvider;
import org.apereo.cas.authentication.bypass.GroovyMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.HttpRequestMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.MultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.PrincipalMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.RegisteredServiceMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.RestMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.metadata.AuthenticationContextAttributeMetaDataPopulator;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProviderBypassProperties;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.configurer.AbstractCasWebflowConfigurer;
import org.apereo.cas.web.flow.configurer.CasMultifactorWebflowCustomizer;
import org.apereo.cas.web.flow.util.MultifactorAuthenticationWebflowUtils;
import org.esupportail.cas.adaptors.esupotp.EsupOtpAuthenticationHandler;
import org.esupportail.cas.adaptors.esupotp.EsupOtpMultifactorAuthenticationProvider;
import org.esupportail.cas.adaptors.esupotp.EsupOtpService;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpAuthenticationWebflowAction;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpAuthenticationWebflowEventResolver;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpGetTransportsAction;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpMultifactorTrustWebflowConfigurer;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpMultifactorWebflowConfigurer;
import org.esupportail.cas.config.support.authentication.EsupOtpBypassProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.webflow.config.FlowDefinitionRegistryBuilder;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.FlowBuilder;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * Dynamically registers one full set of ESUP-OTP MFA beans per indexed configuration entry
 * ({@code esupotp[0]}, {@code esupotp[1]}, ...).
 *
 * <p>For each instance the following beans are registered:
 * <ul>
 *   <li>An {@link EsupOtpService} (plain object, same instance exposed as Spring bean)</li>
 *   <li>An authentication webflow action</li>
 *   <li>A transports (get-methods) webflow action</li>
 *   <li>A {@link MultifactorAuthenticationProvider}</li>
 *   <li>An {@link AuthenticationEventExecutionPlanConfigurer}</li>
 *   <li>A {@link CasWebflowExecutionPlanConfigurer} (main flow + trust flow)</li>
 * </ul>
 *
 * <p>This class must <strong>not</strong> be annotated with {@code @Component}.
 * It is declared as a {@code static @Bean} in {@link EsupOtpAutoConfiguration} so that Spring
 * can instantiate it very early in the context lifecycle — before any {@code @Configuration}
 * classes or regular beans are processed — as required by the
 * {@link BeanDefinitionRegistryPostProcessor} contract.
 */
@Slf4j
public class EsupOtpMultiInstancesConfigurationRegistrar implements BeanDefinitionRegistryPostProcessor,
    ApplicationContextAware, Ordered {

    private ConfigurableApplicationContext applicationContext;
    private BeanDefinitionRegistry registry;

    @Override
    public void setApplicationContext(@NotNull final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull final BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void postProcessBeanFactory(@NotNull final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        final Environment environment = beanFactory.getBean(Environment.class);
        List<EsupOtpConfigurationProperties> configs = Binder.get(environment)
            .bind("esupotp", Bindable.listOf(EsupOtpConfigurationProperties.class))
            .orElse(List.of());

        // If the list is empty, try to read old configuration style with only one config in the file
        // -> assure retro-compatibility with old config file
        if(configs.isEmpty()) {
            EsupOtpConfigurationProperties config = Binder.get(environment)
            .bind("esupotp", Bindable.of(EsupOtpConfigurationProperties.class))
            .orElse(null);
            if(config != null) {
                log.info("ESUP-OTP multi-instances registrar: no indexed configuration found, but found single configuration. Using it as the only instance.");
                if(StringUtils.isEmpty(config.getName())) {
                    config.setName("mfa-esupotp");
                }
                configs = List.of(config);
            } else {
                log.warn("ESUP-OTP registrar: no configuration found.");
            }
        }

        log.info("ESUP-OTP multi-instances registrar: found {} instance(s) to register.", configs.size());

        final Map<String, EsupOtpService> esupOtpServices = new LinkedHashMap<>();

        for (int index = 0; index < configs.size(); index++) {
            final EsupOtpConfigurationProperties config = configs.get(index);
            config.afterPropertiesSet();
            log.info("Registering ESUP-OTP instance [{}] (id='{}', name='{}').",
                index, config.getId(), config.getName());
            final EsupOtpService esupOtpService = registerInstanceBeans(beanFactory, config, index);
            esupOtpServices.putIfAbsent(config.getName(), esupOtpService);
            if(configs.size() == 1) {
                // keep compatibility with old version :
                // esupOtpService bean is registered with a fixed name when there is only one instance,
                // to avoid breaking existing references to it in groovy scripts
                registry.registerAlias(beanName("esupOtpService", config.getName()), "esupOtpService");
            }
        }

        if (!esupOtpServices.isEmpty() && !registry.containsBeanDefinition("esupOtpServices")) {
            final RootBeanDefinition definition = new RootBeanDefinition(Map.class);
            definition.setInstanceSupplier(() -> Collections.unmodifiableMap(new LinkedHashMap<>(esupOtpServices)));
            registry.registerBeanDefinition("esupOtpServices", definition);
            log.debug("Registered 'esupOtpServices' map bean with {} entr{}.",
                esupOtpServices.size(), esupOtpServices.size() == 1 ? "y" : "ies");
        }
    }

    private EsupOtpService registerInstanceBeans(final ConfigurableListableBeanFactory beanFactory,
                                             final EsupOtpConfigurationProperties config,
                                             final int index) {
        final String suffix = config.getName();
        final String authenticationActionBeanName = beanName("esupotpAuthenticationWebflowAction", suffix);
        final String transportsActionBeanName = beanName("esupotpGetTransportsAction", suffix);
        final String providerBeanName = beanName("esupotpAuthenticationProvider", suffix);
        final String authPlanConfigurerBeanName = beanName("esupotpAuthenticationEventExecutionPlanConfigurer", suffix);
        final String webflowPlanConfigurerBeanName = beanName("esupotpCasWebflowExecutionPlanConfigurer", suffix);

        // --- Authentication webflow action ---
        // Resolved lazily: the Supplier is called only when the bean is first requested,
        // at which point CasWebflowEventResolutionConfigurationContext is guaranteed to be ready.
        registerBean(authenticationActionBeanName, Action.class, () -> {
            log.debug("Creating bean '{}' for ESUP-OTP instance '{}'.", authenticationActionBeanName, config.getName());
            final EsupOtpAuthenticationWebflowAction action = new EsupOtpAuthenticationWebflowAction();
            action.setEsupotpAuthenticationWebflowEventResolver(new EsupOtpAuthenticationWebflowEventResolver(
                beanFactory.getBean(org.apereo.cas.web.flow.resolver.impl.CasWebflowEventResolutionConfigurationContext.class)
            ));
            return action;
        });

        // --- EsupOtpService ---
        // Created directly (no Spring dependencies) to avoid forcing premature bean initialization.
        // The same instance is both registered as a named Spring bean and captured in other Suppliers.
        final String esupOtpServiceBeanName = beanName("esupOtpService", suffix);
        final EsupOtpService esupOtpService = new EsupOtpService(config);
        registerBean(esupOtpServiceBeanName, EsupOtpService.class, () -> esupOtpService);
        log.debug("Registered bean '{}' for ESUP-OTP instance '{}'.", esupOtpServiceBeanName, config.getName());

        // --- Transports (get-methods) webflow action ---
        registerBean(transportsActionBeanName, Action.class, () -> {
            log.debug("Creating bean '{}' for ESUP-OTP instance '{}'.", transportsActionBeanName, config.getName());
            return new EsupOtpGetTransportsAction(applicationContext, config, esupOtpService);
        });

        // --- MFA provider ---
        // Dependencies (failureModeEvaluator, bypass evaluators) are resolved lazily inside the Supplier.
        registerBean(providerBeanName, MultifactorAuthenticationProvider.class, () -> {
            log.debug("Creating bean '{}' for ESUP-OTP instance '{}'.", providerBeanName, config.getName());
            final EsupOtpMultifactorAuthenticationProvider provider = new EsupOtpMultifactorAuthenticationProvider();
            provider.setBypassEvaluator(createBypassEvaluator(beanFactory, config, esupOtpService));
            provider.setFailureMode(config.getFailureMode());
            provider.setFailureModeEvaluator(beanFactory.getBean(MultifactorAuthenticationFailureModeEvaluator.class));
            provider.setOrder(config.getRank());
            provider.setId(config.getName());
            return provider;
        });

        // --- Authentication execution plan configurer ---
        // Handler and metadata populator are plain objects (no Spring injection required);
        // they are registered with CAS's internal plan, not as Spring beans.
        registerBean(authPlanConfigurerBeanName, AuthenticationEventExecutionPlanConfigurer.class, () -> plan -> {
            log.debug("Registering authentication handler and metadata populator for ESUP-OTP instance '{}'.", config.getName());
            final AuthenticationHandler handler = new EsupOtpAuthenticationHandler(
                StringUtils.defaultIfBlank(config.getName(), config.getId()),
                beanFactory.getBean("principalFactory", PrincipalFactory.class),
                config,
                esupOtpService
            );
            final AuthenticationMetaDataPopulator metadataPopulator = new AuthenticationContextAttributeMetaDataPopulator(
                beanFactory.getBean(CasConfigurationProperties.class).getAuthn().getMfa().getCore().getAuthenticationContextAttribute(),
                handler,
                config.getId()
            );
            plan.registerAuthenticationHandler(handler);
            plan.registerAuthenticationMetadataPopulator(metadataPopulator);
            log.info("Registered authentication handler '{}' for ESUP-OTP instance '{}'.",
                handler.getName(), config.getName());
        });

        // --- Webflow execution plan configurer ---
        // Registers the main MFA webflow configurer and the trust webflow configurer.
        // All bean lookups are deferred to when the plan is built (lazy).
        registerBean(webflowPlanConfigurerBeanName, CasWebflowExecutionPlanConfigurer.class, () -> plan -> {
            log.debug("Configuring webflow for ESUP-OTP instance '{}'.", config.getName());
            final FlowDefinitionRegistry flowRegistry = createFlowRegistry(beanFactory, config);
            final List<CasMultifactorWebflowCustomizer> customizers =
                MultifactorAuthenticationWebflowUtils.getMultifactorAuthenticationWebflowCustomizers(applicationContext);
            final CasConfigurationProperties casProperties = beanFactory.getBean(CasConfigurationProperties.class);
            final FlowDefinitionRegistry loginFlowDefinitionRegistry =
                beanFactory.getBean(CasWebflowConstants.BEAN_NAME_FLOW_DEFINITION_REGISTRY, FlowDefinitionRegistry.class);
            final FlowBuilderServices flowBuilderServices = beanFactory.getBean(FlowBuilderServices.class);

            final AbstractCasWebflowConfigurer mainConfigurer = new EsupOtpMultifactorWebflowConfigurer(
                flowBuilderServices, loginFlowDefinitionRegistry, flowRegistry,
                applicationContext, casProperties, customizers,
                authenticationActionBeanName, transportsActionBeanName
            );
            mainConfigurer.setOrder(100);
            plan.registerWebflowConfigurer(mainConfigurer);
            log.debug("Registered main webflow configurer for ESUP-OTP instance '{}'.", config.getName());

            final AbstractCasWebflowConfigurer trustConfigurer = new EsupOtpMultifactorTrustWebflowConfigurer(
                flowBuilderServices, loginFlowDefinitionRegistry, flowRegistry,
                applicationContext, casProperties, customizers
            );
            trustConfigurer.setOrder(101);
            plan.registerWebflowConfigurer(trustConfigurer);
            log.debug("Registered trust webflow configurer for ESUP-OTP instance '{}'.", config.getName());
        });

        log.info("All beans registered for ESUP-OTP instance [{}] (name='{}').", index, config.getName());
        return esupOtpService;
    }


    private FlowDefinitionRegistry createFlowRegistry(final ConfigurableListableBeanFactory beanFactory,
                                                      final EsupOtpConfigurationProperties config) {
        log.debug("Building FlowDefinitionRegistry for ESUP-OTP instance '{}'.", config.getName());
        final FlowDefinitionRegistryBuilder builder = new FlowDefinitionRegistryBuilder(
            applicationContext, beanFactory.getBean(FlowBuilderServices.class));
        builder.addFlowBuilder(beanFactory.getBean("flowBuilder", FlowBuilder.class), config.getName());
        return builder.build();
    }

    private MultifactorAuthenticationProviderBypassEvaluator createBypassEvaluator(
            final ConfigurableListableBeanFactory beanFactory,
            final EsupOtpConfigurationProperties config,
            final EsupOtpService service) {
        log.debug("Building bypass evaluator chain for ESUP-OTP instance '{}'.", config.getId());
        final MultifactorAuthenticationProviderBypassProperties props = config.getBypass();
        final ChainingMultifactorAuthenticationProviderBypassEvaluator bypass =
            new DefaultChainingMultifactorAuthenticationBypassProvider(applicationContext);
        final MultifactorAuthenticationFailureModeEvaluator failureModeEvaluator =
            beanFactory.getBean(MultifactorAuthenticationFailureModeEvaluator.class);

        bypass.addMultifactorAuthenticationProviderBypassEvaluator(
            new EsupOtpBypassProvider(service, config, applicationContext, failureModeEvaluator));

        if (StringUtils.isNotBlank(props.getPrincipalAttributeName())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(
                new PrincipalMultifactorAuthenticationProviderBypassEvaluator(props, config.getId(), applicationContext));
            log.debug("Added PrincipalMultifactorAuthenticationProviderBypassEvaluator for instance '{}'.", config.getId());
        }
        bypass.addMultifactorAuthenticationProviderBypassEvaluator(
            new RegisteredServiceMultifactorAuthenticationProviderBypassEvaluator(config.getId(), applicationContext));

        if (StringUtils.isNotBlank(props.getAuthenticationAttributeName())
            || StringUtils.isNotBlank(props.getAuthenticationHandlerName())
            || StringUtils.isNotBlank(props.getAuthenticationMethodName())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(
                new AuthenticationMultifactorAuthenticationProviderBypassEvaluator(props, config.getId(), applicationContext));
            log.debug("Added AuthenticationMultifactorAuthenticationProviderBypassEvaluator for instance '{}'.", config.getId());
        }
        if (StringUtils.isNotBlank(props.getCredentialClassType())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(
                new CredentialMultifactorAuthenticationProviderBypassEvaluator(props, config.getId(), applicationContext));
            log.debug("Added CredentialMultifactorAuthenticationProviderBypassEvaluator for instance '{}'.", config.getId());
        }
        if (StringUtils.isNotBlank(props.getHttpRequestHeaders()) || StringUtils.isNotBlank(props.getHttpRequestRemoteAddress())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(
                new HttpRequestMultifactorAuthenticationProviderBypassEvaluator(props, config.getId(), applicationContext));
            log.debug("Added HttpRequestMultifactorAuthenticationProviderBypassEvaluator for instance '{}'.", config.getId());
        }
        if (props.getGroovy().getLocation() != null) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(
                new GroovyMultifactorAuthenticationProviderBypassEvaluator(props, config.getId(), applicationContext));
            log.debug("Added GroovyMultifactorAuthenticationProviderBypassEvaluator for instance '{}'.", config.getId());
        }
        if (StringUtils.isNotBlank(props.getRest().getUrl())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(
                new RestMultifactorAuthenticationProviderBypassEvaluator(props, config.getId(), applicationContext));
            log.debug("Added RestMultifactorAuthenticationProviderBypassEvaluator for instance '{}'.", config.getId());
        }
        return bypass;
    }

    /**
     * Registers a bean definition only if no bean with the given name already exists.
     * The actual instance is created lazily by the provided {@link Supplier}, i.e. only
     * when the bean is first requested from the application context.
     */
    private <T> void registerBean(final String beanName, final Class<T> beanType, final Supplier<T> supplier) {
        if (registry.containsBeanDefinition(beanName)) {
            log.debug("Bean '{}' already registered, skipping.", beanName);
            return;
        }
        final RootBeanDefinition definition = new RootBeanDefinition(beanType);
        definition.setInstanceSupplier(supplier);
        registry.registerBeanDefinition(beanName, definition);
        log.debug("Bean definition '{}' ({}) queued for lazy instantiation.", beanName, beanType.getSimpleName());
    }

    private String beanName(final String baseName, final String suffix) {
        return baseName + "-" + suffix;
    }
}
