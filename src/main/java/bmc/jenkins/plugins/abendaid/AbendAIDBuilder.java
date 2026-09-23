package bmc.jenkins.plugins.abendbuild.mybuilder;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AbendAIDBuilder extends Builder implements SimpleBuildStep {

    private final String name; // connection information for the API
    private final Secret token; // token used for permission checking
    private final String abendAPI; // the selected API that the user wants
    private final int reportNum; // report number for diagnostic summary

    @DataBoundConstructor
    public AbendAIDBuilder(String name, Secret token, String abendAPI, int reportNum)
            throws hudson.model.Descriptor.FormException {
        if (abendAPI == null || abendAPI.trim().isEmpty()) {
            throw new hudson.model.Descriptor.FormException(
                    "You must choose a valid API request before saving.", "abendAPI");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new hudson.model.Descriptor.FormException("You must set a configuration.", "name");
        }
        if (token == null || token.getPlainText().isEmpty()) {
            throw new hudson.model.Descriptor.FormException("You must set a token.", "token");
        }
        if (abendAPI.equals("report")) {
            if (reportNum == 0) {
                throw new hudson.model.Descriptor.FormException("You must set a report number.", "reportNum");
            }
        }
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
        if (abendAPI.equals("query")) {
            URIabend = URI.create("http://" + name + "/compuware/ws/abendaidapi/" + abendAPI);
        }
        // request for directory
        if (abendAPI.equals("report")) {
            URIabend =
                    URI.create("http://" + name + "/compuware/ws/abendaidapi/diagnosticsummary?data=RPT=" + reportNum);
        }
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
                FilePath targetFile = workspace.child("Abend_AID_API/API" + buildnumber + ".txt");

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

        @POST
        public FormValidation doCheckAbendAPI(@QueryParameter String value, @AncestorInPath Item item) {

            if (item == null) { // no context
                return FormValidation.ok();
            }
            if (!item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("You must choose a valid API request");
            }

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }
}
