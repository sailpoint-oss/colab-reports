# Business Role Required and Permitted Report

Business Role Report that displays the required roles, permitted roles, and assignment rules for each business role.

### Usage
- Compile the file *BusinessRoleReqPermReport.java* using:

      javac -cp "<SailPoint-Root-Folder>/WEB-INF/lib/identityiq.jar" BusinessRoleReqPermReport.java
- Copy the compiled *BusinessRoleReqPermReport.class* file to correct classes folder

      cp BusinessRoleReqPermReport.class <SailPoint-Root-Folder>/WEB-INF/classes/sailpoint/reporting/datasource
- Restart the IdentityIQ application server (i.e. restart Tomcat)

- Import the file *TaskDefinition-BusinessRoleReqPermReport.xml* into IdentityIQ
    
  - Gear->Global Settings->Import from File
- Find the report *Business Role Required and Permitted Report* in Intelligence->Reports category *Role Management Reports*