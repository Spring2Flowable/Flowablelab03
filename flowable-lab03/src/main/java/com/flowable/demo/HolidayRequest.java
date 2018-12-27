package com.flowable.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

/**
 * Hello world!
 *
 */
public class HolidayRequest {
	public static ProcessEngineConfiguration cfg = null;
	public static ProcessEngine processEngine = null;
	public static RepositoryService repositoryService = null;
	public static RuntimeService runtimeService = null;
	public static TaskService taskService = null;
	public static ProcessInstance processInstance = null;
	
	public static Scanner scanner = null;

	public static String employee = null;

	public static Integer nrOfHolidays = null;

	public static String description = null;
	
	static {
		
		scanner = new Scanner(System.in);
		
		// 初始化流程配置
		initProcess();
	}

	/**
	 * @param args
	 * 主方法
	 */
	public static void main(String[] args) {

		// 交互式开始流程
		interactionProcess();

		// 开始流程
		startProcess();

		// 查询任务
		queryTasks();
		
		//查询流程历史记录
		queryHistoryData();

	}

	/**
	 * 流程配置
	 */
	public static void initProcess() {
		// 配置流程数据库
		cfg = new StandaloneProcessEngineConfiguration().setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
				.setJdbcUsername("sa").setJdbcPassword("").setJdbcDriver("org.h2.Driver")
				.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
		processEngine = cfg.buildProcessEngine();

		// 获取流程静态资源
		repositoryService = processEngine.getRepositoryService();

		// 将流程定义部署到Flowable引擎
		Deployment deployment = repositoryService.createDeployment().addClasspathResource("holiday-request.bpmn20.xml")
				.deploy();

		// 查询流程引擎
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.deploymentId(deployment.getId()).singleResult();

		System.out.println("Found process definition of name : " + processDefinition.getName());
		System.out.println("Found process definition of deploymentId : " + processDefinition.getDeploymentId());
		System.out.println("Found process definition of key : " + processDefinition.getKey());
		System.out.println("Found process definition of engineVersion : " + processDefinition.getEngineVersion());
	}

	/**
	 * 流程交互：开始流程
	 */
	public static void interactionProcess() {

		System.out.println("Who are you?");
		employee = scanner.nextLine();

		System.out.println("How many holidays do you want to request?");
		nrOfHolidays = Integer.valueOf(scanner.nextLine());

		System.out.println("Why do you need them?");
		description = scanner.nextLine();
	}

	/**
	 * 启动流程
	 */
	public static void startProcess() {
		runtimeService = processEngine.getRuntimeService();

		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("employee", employee);
		variables.put("nrOfHolidays", nrOfHolidays);
		variables.put("description", description);

		processInstance = runtimeService.startProcessInstanceByKey("holidayRequest", variables);

	}

	/**
	 * 查询managers任务列表
	 */
	public static void queryTasks() {
		taskService = processEngine.getTaskService();

		List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();

		System.out.println("You have " + tasks.size() + " tasks:");

		for (int i = 0; i < tasks.size(); i++) {
			
			System.out.println((i + 1) + ") " + tasks.get(i).getName());
			
		}
		
		getProcessVariables(tasks);
	}

	/**
	 * 获取流程变量
	 */
	public static void getProcessVariables(List<Task> tasks ) {
		
		System.out.println("Which task would you like to complete?");
		
		int taskIndex = Integer.valueOf(scanner.nextLine());
		Task task = tasks.get(taskIndex - 1);

		Map<String, Object> processVariables = taskService.getVariables(task.getId());
		System.out.println(processVariables.get("employee") + " wants " + processVariables.get("nrOfHolidays")
				+ " of holidays. Do you approve this?");
		
		//完成流程
		completeProcess(task);

	}
	/**
	 * 完成流程
	 */
	public static void completeProcess(Task task) {
		
		
		
		boolean approved = scanner.nextLine().toLowerCase().equals("y");
		
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("approved", approved);
		taskService.complete(task.getId(), variables);

	}
	/**
	 * 查询流程历史记录
	 */
	public static void queryHistoryData() {
		
		HistoryService historyService = processEngine.getHistoryService();
		List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstance.getId()).finished().orderByHistoricActivityInstanceEndTime().asc()
				.list();

		for (HistoricActivityInstance activity : activities) {
			System.out.println(activity.getActivityId() + " took " + activity.getDurationInMillis() + " milliseconds");
		}
	}
}
