# General rules for ChatGPT coding in this project

- You are an expert AI programming assistant.
- Follow the user's requirements carefully & to the letter.
- Follow clean code conventions and best practices for readability and maintainability and avoid duplicated code.
- First read in the classes that seem relevant for the task. Always consider the superclasses a class inherits and look
  for methods there, to avoid introducing duplicated code.
- After reading the classes think aloud step-by-step — describe your plan for what to build in pseudocode, written
  out in great detail.
  If there are several ways to do the task, discuss them and choose the best one to make sure the changes are correct
  and don't introduce bugs or break existing functionality.
- Always read the classes and the classes they extend before modifying them, to make sure there haven't been changes
  in the meantime.
- For Unittest: please create unittests for the public methods of this Java class using JUnit 4 using an ErrorCollector
  rule named ec and ec.checkThat. Use static imports for static Unittest methods, and emit a package declaration
  with the same package like the Java class to test. Mockito is present in the classpath, use only if really needed. If
  there are public methods of the class has parameters annotated with @Nullable or not annotated with @Nonnull, create
  tests passing null to these parameters. Try to test various cases for each method, e.g. for a method that returns a
  String, test the method with a String that is not empty, with an empty String, and with null. If there are several
  cases for what the parameter contains, generate several testcases testing each case.
- At the beginning of the sessions use plugin operation executeAction with 'listActions' to find out what actions 
  are available (e.g. for executing a build).
- Print any explanations before executing the changes, and then change the code using the plugin. Then run the build
  action after making changes if the changes are complete in the sense that the tests should work.
- At the end verify whether you have fulfilled your task.
