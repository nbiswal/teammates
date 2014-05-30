package teammates.test.cases.ui;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.util.Const;
import teammates.logic.core.CoursesLogic;
import teammates.ui.controller.InstructorCourseDeleteAction;
import teammates.ui.controller.RedirectResult;

public class InstructorCourseDeleteActionTest extends BaseActionTest {

    DataBundle dataBundle;
    
    
    @BeforeClass
    public static void classSetUp() throws Exception {
        printTestClassHeader();
        uri = Const.ActionURIs.INSTRUCTOR_COURSE_DELETE;
    }

    @BeforeMethod
    public void caseSetUp() throws Exception {
        dataBundle = getTypicalDataBundle();
        restoreTypicalDataInDatastore();
    }
    
    @Test
    public void testAccessControl() throws Exception{
        
        CoursesLogic.inst().createCourseAndInstructor(
                dataBundle.instructors.get("instructor1OfCourse1").googleId, 
                "icdat.owncourse", "New course");
        
        String[] submissionParams = new String[]{
                Const.ParamsNames.COURSE_ID, "icdat.owncourse"
        };

        /*  Test access for users 
         *  This should be separated from testing for admin as we need to recreate the course after being removed         
         */
        verifyUnaccessibleWithoutLogin(submissionParams);
        verifyUnaccessibleForUnregisteredUsers(submissionParams);
        verifyUnaccessibleForStudents(submissionParams);
        verifyUnaccessibleForInstructorsOfOtherCourses(submissionParams);
        verifyAccessibleForInstructorsOfTheSameCourse(submissionParams);

        /* Test access for admin in masquerade mode */
        CoursesLogic.inst().createCourseAndInstructor(
                dataBundle.instructors.get("instructor1OfCourse1").googleId, 
                "icdat.owncourse", "New course");
        verifyAccessibleForAdminToMasqueradeAsInstructor(submissionParams);
    }
    
    @Test
    public void testExecuteAndPostProcess() throws Exception{
        //TODO: find a way to test status message from session
        InstructorAttributes instructor1OfCourse1 = dataBundle.instructors.get("instructor1OfCourse1");
        String instructorId = instructor1OfCourse1.googleId;

        gaeSimulation.loginAsInstructor(instructorId);
        
        ______TS("Not enough parameters");
        verifyAssumptionFailure();

        ______TS("Typical case, 2 courses, redirect to homepage");
        CoursesLogic.inst().createCourseAndInstructor(instructorId, "icdct.tpa.id1", "New course");
        String[] submissionParams = new String[]{
                Const.ParamsNames.COURSE_ID, instructor1OfCourse1.courseId,
                Const.ParamsNames.NEXT_URL, Const.ActionURIs.INSTRUCTOR_HOME_PAGE
        };
        
        assertEquals(true, CoursesLogic.inst().isCoursePresent("icdct.tpa.id1"));
        InstructorCourseDeleteAction deleteAction = getAction(submissionParams);
        RedirectResult redirectResult = getRedirectResult(deleteAction);
        
        assertEquals(
                Const.ActionURIs.INSTRUCTOR_HOME_PAGE+"?error=false&user=idOfInstructor1OfCourse1", 
                redirectResult.getDestinationWithParams());
        assertEquals(false, redirectResult.isError);
        assertEquals("The course idOfTypicalCourse1 has been deleted.", redirectResult.getStatusMessage());
        
        List<CourseAttributes> courseList = CoursesLogic.inst().getCoursesForInstructor(instructorId);
        assertEquals(1, courseList.size());
        assertEquals("icdct.tpa.id1", courseList.get(0).id);

        String expectedLogMessage = "TEAMMATESLOG|||instructorCourseDelete" +
                "|||instructorCourseDelete|||true|||Instructor|||Instructor 1 of Course 1" +
                "|||idOfInstructor1OfCourse1|||instr1@course1.com" +
                "|||Course deleted: idOfTypicalCourse1|||/page/instructorCourseDelete";
        assertEquals(expectedLogMessage, deleteAction.getLogMessage());
        
        ______TS("Masquerade mode, delete last course, redirect to Courses page");
        
        gaeSimulation.loginAsAdmin("admin.user");
        submissionParams = new String[]{
                Const.ParamsNames.COURSE_ID, "icdct.tpa.id1",
                Const.ParamsNames.NEXT_URL, Const.ActionURIs.INSTRUCTOR_COURSES_PAGE
        };
        deleteAction = getAction(addUserIdToParams(instructorId, submissionParams));
        redirectResult = getRedirectResult(deleteAction);
        
        assertEquals(
                Const.ActionURIs.INSTRUCTOR_COURSES_PAGE+"?error=false&user=idOfInstructor1OfCourse1", 
                redirectResult.getDestinationWithParams());
        assertEquals(false, redirectResult.isError);
        assertEquals("The course icdct.tpa.id1 has been deleted.", redirectResult.getStatusMessage());
        
        courseList = CoursesLogic.inst().getCoursesForInstructor(instructorId);
        assertEquals(0, courseList.size());
        
        expectedLogMessage = "TEAMMATESLOG|||instructorCourseDelete|||instructorCourseDelete" +
                "|||true|||Instructor(M)|||Instructor 1 of Course 1|||idOfInstructor1OfCourse1" +
                "|||instr1@course1.com|||Course deleted: icdct.tpa.id1|||/page/instructorCourseDelete";
        assertEquals(expectedLogMessage, deleteAction.getLogMessage());
    }
    
    
    private InstructorCourseDeleteAction getAction(String... params) throws Exception{
            return (InstructorCourseDeleteAction) (gaeSimulation.getActionObject(uri, params));
    }
    

}
