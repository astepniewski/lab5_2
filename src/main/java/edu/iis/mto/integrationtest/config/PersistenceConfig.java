package edu.iis.mto.integrationtest.config;

import java.sql.Driver;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import edu.iis.mto.integrationtest.main.ApplicationMain;
import edu.iis.mto.integrationtest.utils.ModeUtils;

@Configuration
// wskazuje, �e klasa zawiera konfiguracj� bean��w Spring
@EnableTransactionManagement
// umo�lwia konfigurowanie mechanizm�w transakcji z wykorzystaniem adnotacji
@EnableJpaRepositories(basePackages = { "edu.iis.mto.integrationtest.repository" })
// okre�la gdzie szuka� klas definiuj�cych repozytoria Spring Data.
public class PersistenceConfig {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ApplicationMain.class);

	private final String SQL_SCHEMA_SCRIPT_PATH = "sql/schema-script.sql";
	private final String SQL_FOLDER_NAME = "sql/";
	private final String DATA_SCRIPT_FILENAME_SUFFIX = "-data-script.sql";

	@Value("${database.url}")
	private String databaseUrl;

	@Value("${database.user}")
	private String databaseUser;

	@Value("${database.password}")
	private String databasePassword;

	@Value("${database.driver}")
	private String databaseDriverClass;

	@Bean(name = "dataSource")
	public DataSource getDataSource() {
		DataSource dataSource = createDataSource();
		DatabasePopulatorUtils.execute(createDatabasePopulator(), dataSource);
		return dataSource;
	}

	private DatabasePopulator createDatabasePopulator() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.setContinueOnError(true);
		databasePopulator.addScripts(new ClassPathResource(
				SQL_SCHEMA_SCRIPT_PATH), new ClassPathResource(SQL_FOLDER_NAME
				+ ModeUtils.getMode().getModeName()
				+ DATA_SCRIPT_FILENAME_SUFFIX));
		return databasePopulator;
	}

	private SimpleDriverDataSource createDataSource() {
		SimpleDriverDataSource simpleDriverDataSource = new SimpleDriverDataSource();
		Class<? extends Driver> driverClass = getDriverClass();
		simpleDriverDataSource.setDriverClass(driverClass);
		simpleDriverDataSource.setUrl(databaseUrl);
		simpleDriverDataSource.setUsername(databaseUser);
		simpleDriverDataSource.setPassword(databasePassword);
		return simpleDriverDataSource;
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Driver> getDriverClass() {
		try {
			Class<?> driverClass = Class.forName(databaseDriverClass);
			if (Driver.class.isAssignableFrom(driverClass)) {
				return (Class<? extends Driver>) driverClass;
			} else {
				LOGGER.error("database driver class is not the SQL driver ");
				return null;
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("database driver class not found", e);
			return null;
		}
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(getDataSource());
		entityManagerFactoryBean
				.setPackagesToScan("edu.iis.mto.integrationtest.model");
		entityManagerFactoryBean
				.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		return entityManagerFactoryBean;
	}

	@Bean(name = "transactionManager")
	public PlatformTransactionManager annotationDrivenTransactionManager() {
		return new JpaTransactionManager();
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
		return new PersistenceExceptionTranslationPostProcessor();
	}
}
