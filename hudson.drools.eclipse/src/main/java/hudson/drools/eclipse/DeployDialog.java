package hudson.drools.eclipse;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public abstract class DeployDialog extends Dialog {

	protected Text hudsonUrl;
	protected Combo projectName;
	private final String title;
	protected Text userName;
	protected Text password;

	protected DeployDialog(String title, Shell parentShell) {
		super(parentShell);
		this.title = title;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(title);
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		
		Composite composite = (Composite) super.createDialogArea(parent);

		FormLayout layout = new FormLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		composite.setLayout(layout);

		Label hudsonUrlLabel = new Label(composite, SWT.NONE);
		FormData formData = new FormData();
		formData.top = new FormAttachment(0, 5);
		formData.left = new FormAttachment(0, 5);
		hudsonUrlLabel.setLayoutData(formData);
		hudsonUrlLabel.setText("Hudson URL");

		hudsonUrl = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		formData = new FormData();
		formData.top = new FormAttachment(hudsonUrlLabel, 0, SWT.TOP);
		formData.left = new FormAttachment(hudsonUrlLabel, 5);
		formData.right = new FormAttachment(100, -5);
		hudsonUrl.setLayoutData(formData);
		String hudsonUrlValue = settings.get("hudsonUrl");
		if (hudsonUrlValue == null) {
			hudsonUrlValue = Hudson.discover();
		}
		if (hudsonUrlValue != null) {
			hudsonUrl.setText(hudsonUrlValue);
		}
		
		userName = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		formData = new FormData();
		formData.top = new FormAttachment(hudsonUrl, 5);
		formData.left = new FormAttachment(hudsonUrl, 0, SWT.LEFT);
		formData.right = new FormAttachment(100, -5);
		userName.setLayoutData(formData);
		String userNameValue = settings.get("userName");
		if (userNameValue != null) {
			userName.setText(userNameValue);
		}

		Label userNameLabel = new Label(composite, SWT.NONE);
		formData = new FormData();
		formData.top = new FormAttachment(userName, 0, SWT.TOP);
		formData.left = new FormAttachment(0, 5);
		formData.right = new FormAttachment(userName, 5);
		userNameLabel.setLayoutData(formData);
		userNameLabel.setText("User");

		password = new Text(composite, SWT.SINGLE | SWT.LEAD | SWT.BORDER | SWT.PASSWORD);
		formData = new FormData();
		formData.top = new FormAttachment(userName, 5);
		formData.left = new FormAttachment(userName, 0, SWT.LEFT);
		formData.right = new FormAttachment(100, -5);
		password.setLayoutData(formData);
		String passwordValue = settings.get("password");
		if (passwordValue != null) {
			password.setText(passwordValue);
		}
		
		Label passwordLabel = new Label(composite, SWT.NONE);
		formData = new FormData();
		formData.top = new FormAttachment(password, 0, SWT.TOP);
		formData.left = new FormAttachment(0, 5);
		formData.right = new FormAttachment(password, 5);
		passwordLabel.setLayoutData(formData);
		passwordLabel.setText("Password");

		projectName = new Combo(composite, SWT.SINGLE | SWT.LEAD
				| SWT.BORDER);
		formData = new FormData();
		formData.top = new FormAttachment(password, 5);
		formData.left = new FormAttachment(password, 0, SWT.LEFT);
		formData.right = new FormAttachment(100, -5);
		projectName.setLayoutData(formData);
		String projectNameValue = settings.get("projectName");
		if (projectNameValue != null)
		projectName.setText(projectNameValue);

		Label projectNameLabel = new Label(composite, SWT.NONE);
		formData = new FormData();
		formData.top = new FormAttachment(projectName, 0, SWT.TOP);
		formData.left = new FormAttachment(0, 5);
		formData.right = new FormAttachment(projectName, 5);
		projectNameLabel.setLayoutData(formData);
		projectNameLabel.setText("Project");

		FocusListener focusListener = new FocusListener() {
			public void focusGained(FocusEvent arg0) {
			}

			public void focusLost(FocusEvent ev) {
				Hudson hudson = new Hudson(hudsonUrl.getText(), userName.getText(), password.getText());
				if (!hudson.verify()) {
					hudsonUrl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					projectName.setItems(new String[] { "<hudson url invalid>"});
					getButton(IDialogConstants.OK_ID).setEnabled(false);
				} else {
					hudsonUrl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
					try {
						String selected = projectName.getText();
						List<String> projects= hudson.getWorkflowProjects();
						projectName.setItems((String[]) projects
								.toArray(new String[projects.size()]));
						if (selected != null) {
							int selectedIndex = projects.indexOf(selected);
							if (selectedIndex >= 0) {
								projectName.select(selectedIndex);
							}
						}
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		};		
		
		hudsonUrl.addFocusListener(focusListener);
		userName.addFocusListener(focusListener);
		password.addFocusListener(focusListener);
		
		return composite;
	}
	
}
