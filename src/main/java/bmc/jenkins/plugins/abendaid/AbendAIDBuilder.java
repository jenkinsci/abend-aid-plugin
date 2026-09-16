package bmc.jenkins.plugins.abendbuild.mybuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jenkins.tasks.SimpleBuildStep;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol; 
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import hudson.util.ListBoxModel;
import hudson.ProxyConfiguration;
import org.kohsuke.stapler.verb.POST;

public class AbendAIDBuilder extends Builder implements SimpleBuildStep {

    private final String name; // connection information for the API
    private final Secret token; // token used for permission checking
    private final String abendAPI; // the selected API that the user wants
    private final int reportNum; // report number for diagnostic summary        

    @DataBoundConstructor
    public AbendAIDBuilder(String name, Secret token, String abendAPI, int reportNum) {
        this.name = name;
        this.token = token;
        this.abendAPI = abendAPI;
        this.reportNum = reportNum;
    }

    public String getName() {
        return name;
    }
    public Secret getToken() {
        return this.token;
    }
        public String getAPI() {
        return abendAPI;
    }
        public int getReport() {
        return reportNum;
    }

// The code below will proccess the request made by the user.
// They can select diagnostic summary or the directory
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {


        // set up the URI for the request
        URI URIabend = URI.create("test");
        HttpClient client = ProxyConfiguration.newHttpClientBuilder().build();
            listener.getLogger().println("API: " + abendAPI);
            listener.getLogger().println("report: " + reportNum);
        // request for diagnostic summary
        if (abendAPI.equals("query")){
            URIabend = URI.create("http://"+name+"/compuware/ws/abendaidapi/"+abendAPI);}
        // request for directory 
        if (abendAPI.equals("report")){
            URIabend = URI.create("http://"+name+"/compuware/ws/abendaidapi/diagnosticsummary?data=RPT="+reportNum);}
            String tokenstr = Secret.toString(token);
            listener.getLogger().println("URIabend: " + URIabend);
            // request the API
            HttpRequest request = ProxyConfiguration.newHttpRequestBuilder(URIabend)
                .header("Content-Type", "application/json")
                .header("Authorization", tokenstr)
                .GET() 
                .build();

        try {
            // set up the listener
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            listener.getLogger().println("API: " + abendAPI);
            listener.getLogger().println("report: " + reportNum);
            listener.getLogger().println("Response" + response.body());
            String responseBody = response.body();
            try {
            // This will build the file to hold the API request for future use
            int buildnumber = run.getNumber();
            FilePath targetFile = workspace.child("Abend_AID_API/API"+buildnumber+".txt");

            targetFile.write(responseBody, "UTF-8");



        } catch (IOException e) {
            listener.error("Failed to write file due to I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            listener.error("File writing execution was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); 
        }

        } catch (IOException | InterruptedException e) {

        }
      
            listener.getLogger().println("connection, " + name);
    
       
    }
    // This will create the seletor for the drop down for the specific request
    @Extension
    @Symbol("abendAid") 
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public ListBoxModel doFillAbendAPIItems() {
            ListBoxModel items = new ListBoxModel();
            

            items.add("Select API Request", ""); 
            items.add("Directory", "query");
            items.add("Diagnostic Summary", "report");
            
            return items;
        } 

        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }

            if (value.length() == 0)
                return FormValidation.error(Messages.errors_missingName());

            return FormValidation.ok();
        }
        public FormValidation doCheckToken(@QueryParameter String value)
                throws IOException, ServletException {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }

            if (value.length() == 0)
                return FormValidation.error(Messages.errors_missingToken());

            return FormValidation.ok();
        }
        public FormValidation doCheckReportNum(@QueryParameter String value)
                throws IOException, ServletException {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }

            if (value.length() == 0)
                return FormValidation.error(Messages.errors_missingReport());

            return FormValidation.ok();
        }
        @POST
        public FormValidation doCheckAbendAPI(@QueryParameter String value)
                throws IOException, ServletException {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            } 
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error(Messages.errors_missingRequest());
            }
            

            return FormValidation.ok();
        }
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

    }


}
