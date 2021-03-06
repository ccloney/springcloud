package top.cg0509.task.controller;

import java.util.HashMap;
import java.util.Map;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.pagehelper.PageInfo;
import top.cg0509.task.entity.JobAndTrigger;
import top.cg0509.task.job.BaseJob;
import top.cg0509.task.service.IJobAndTriggerService;

import javax.annotation.Resource;

@Api(value = "JobController" , description = "定时任务管理")
@RequestMapping(value="/job")
public class JobController 
{
	@Resource
	private IJobAndTriggerService iJobAndTriggerService;
	
	//加入Qulifier注解，通过名称注入bean
	@Autowired @Qualifier("Scheduler")
	private Scheduler scheduler;
	
	private static Logger log = LoggerFactory.getLogger(JobController.class);


	/**
	 * 新增任务
	 * @param jobClassName
	 * @param jobGroupName
	 * @param cronExpression
	 * @throws Exception
	 */
	@ApiOperation(value = "新增定时任务",notes = "")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "jobClassName",value = "任务名称(类名)",required = true),
			@ApiImplicitParam(name = "jobGroupName",value = "分组名称",required = true),
			@ApiImplicitParam(name = "cronExpression",value = "corn表达式",required = true)
	})
	@PostMapping(value="/addjob")
	public void addjob(@RequestParam(value="jobClassName")String jobClassName, 
			@RequestParam(value="jobGroupName")String jobGroupName, 
			@RequestParam(value="cronExpression")String cronExpression) throws Exception
	{			
		addJob(jobClassName, jobGroupName, cronExpression);
	}
	
	public void addJob(String jobClassName, String jobGroupName, String cronExpression)throws Exception{
        
        // 启动调度器  
		scheduler.start(); 
		
		//构建job信息
		JobDetail jobDetail = JobBuilder.newJob(getClass(jobClassName).getClass()).withIdentity(jobClassName, jobGroupName).build();
		
		//表达式调度构建器(即任务执行的时间)
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);

        //按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobClassName, jobGroupName)
            .withSchedule(scheduleBuilder).build();
        try {
        	scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            System.out.println("创建定时任务失败"+e);
            throw new Exception("创建定时任务失败");
        }
	}

	/**
	 *  暂停任务
	 * @param jobClassName
	 * @param jobGroupName
	 * @throws Exception
	 */
	@ApiOperation(value = "暂停任务")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "jobClassName",value = "任务名称",required = true),
			@ApiImplicitParam(name = "jobGroupName",value = "分组名称",required = true)
	})
	@PostMapping(value="/pausejob")
	public void pausejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) throws Exception
	{			
		jobPause(jobClassName, jobGroupName);
	}
	
	public void jobPause(String jobClassName, String jobGroupName) throws Exception
	{	
		scheduler.pauseJob(JobKey.jobKey(jobClassName, jobGroupName));
	}


	/**
	 * 恢复任务
	 * @param jobClassName
	 * @param jobGroupName
	 * @throws Exception
	 */
	@ApiOperation(value = "恢复任务")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "jobClassName",value = "任务名称",required = true),
			@ApiImplicitParam(name = "jobGroupName",value = "分组名称",required = true)
	})
	@PostMapping(value="/resumejob")
	public void resumejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) throws Exception
	{			
		jobresume(jobClassName, jobGroupName);
	}
	
	public void jobresume(String jobClassName, String jobGroupName) throws Exception
	{
		scheduler.resumeJob(JobKey.jobKey(jobClassName, jobGroupName));
	}
	
	@ApiOperation(value = "修改任务执行时间")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "jobClassName",value = "任务名称",required = true),
			@ApiImplicitParam(name = "jobGroupName",value = "分组名称",required = true),
			@ApiImplicitParam(name = "cronExpression",value = "corn表达式",required = true)
	})
	@PostMapping(value="/reschedulejob")
	public void rescheduleJob(@RequestParam(value="jobClassName")String jobClassName, 
			@RequestParam(value="jobGroupName")String jobGroupName,
			@RequestParam(value="cronExpression")String cronExpression) throws Exception
	{			
		jobreschedule(jobClassName, jobGroupName, cronExpression);
	}
	
	public void jobreschedule(String jobClassName, String jobGroupName, String cronExpression) throws Exception
	{				
		try {
			TriggerKey triggerKey = TriggerKey.triggerKey(jobClassName, jobGroupName);
			// 表达式调度构建器
			CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);

			CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

			// 按新的cronExpression表达式重新构建trigger
			trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

			// 按新的trigger重新设置job执行
			scheduler.rescheduleJob(triggerKey, trigger);
		} catch (SchedulerException e) {
			System.out.println("更新定时任务失败"+e);
			throw new Exception("更新定时任务失败");
		}
	}


	/**
	 * 删除任务
	 * @param jobClassName
	 * @param jobGroupName
	 * @throws Exception
	 */
	@ApiOperation(value = "删除任务")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "jobClassName",value = "任务名称",required = true),
			@ApiImplicitParam(name = "jobGroupName",value = "分组名称",required = true)
	})
	@PostMapping(value="/deletejob")
	public void deletejob(@RequestParam(value="jobClassName")String jobClassName, @RequestParam(value="jobGroupName")String jobGroupName) throws Exception
	{			
		jobdelete(jobClassName, jobGroupName);
	}
	
	public void jobdelete(String jobClassName, String jobGroupName) throws Exception
	{		
		scheduler.pauseTrigger(TriggerKey.triggerKey(jobClassName, jobGroupName));
		scheduler.unscheduleJob(TriggerKey.triggerKey(jobClassName, jobGroupName));
		scheduler.deleteJob(JobKey.jobKey(jobClassName, jobGroupName));				
	}

	/**
	 * 查询任务
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	@ApiOperation(value = "查询任务")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "pageNum",value = "起始页",required = true),
			@ApiImplicitParam(name = "pageSize",value = "每天显示行数",required = true)
	})
	@GetMapping(value="/queryjob")
	public Map<String, Object> queryjob(@RequestParam(value="pageNum")Integer pageNum, @RequestParam(value="pageSize")Integer pageSize) 
	{			
		PageInfo<JobAndTrigger> jobAndTrigger = iJobAndTriggerService.getJobAndTriggerDetails(pageNum, pageSize);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("JobAndTrigger", jobAndTrigger);
		map.put("number", jobAndTrigger.getTotal());
		return map;
	}
	
	public static BaseJob getClass(String classname) throws Exception
	{
		Class<?> class1 = Class.forName(classname);
		return (BaseJob)class1.newInstance();
	}
	
	
}
