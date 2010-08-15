package hudson.drools.eclipse;

import java.io.IOException;
import java.net.URLEncoder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class DeployAction implements IObjectActionDelegate {

	private Shell shell;

	private IFile selectedFile;

	/**
	 * Constructor for Action1.
	 */
	public DeployAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		new DeployDialog("Deploy Workflow", shell) {
			@Override
			protected void okPressed() {
				try {
					IDialogSettings settings = Activator.getDefault()
							.getDialogSettings();
					settings.put("hudsonUrl", hudsonUrl.getText());
					settings.put("projectName", projectName.getText());
					settings.put("userName", userName.getText());
					settings.put("password", password.getText());

					new Hudson(hudsonUrl.getText(), userName.getText(),
							password.getText()).deploy(projectName.getText(),
							selectedFile);
				} catch (IOException e) {
					Activator.getDefault().getLog().log(
							new Status(IStatus.ERROR, Activator.PLUGIN_ID, e
									.getMessage(), e));
					MessageDialog.openError(getShell(),
							"Deploy Workflow Error", e.getMessage());
				} catch (CoreException e) {
					Activator.getDefault().getLog().log(
							new Status(IStatus.ERROR, Activator.PLUGIN_ID, e
									.getMessage(), e));
					MessageDialog.openError(getShell(),
							"Deploy Workflow Error", e.getMessage());
				} catch (NoSuchProjectException e) {
					Activator.getDefault().getLog().log(
							new Status(IStatus.ERROR, Activator.PLUGIN_ID, e
									.getMessage(), e));
					MessageDialog.openError(getShell(),
							"Deploy Workflow Error", e.getMessage());
				} catch (NotADroolsProjectException e) {
					Activator.getDefault().getLog().log(
							new Status(IStatus.ERROR, Activator.PLUGIN_ID, e
									.getMessage(), e));
					MessageDialog.openError(getShell(),
							"Deploy Workflow Error", e.getMessage());
				}

				super.okPressed();

			}

		}.open();
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		selectedFile = (IFile) ((StructuredSelection) selection)
				.getFirstElement();
	}
	
}
