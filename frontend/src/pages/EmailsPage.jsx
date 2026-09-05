import { useState } from 'react';
import EmailTable from '../components/EmailTable';
import EmailDetailDrawer from '../components/EmailDetailDrawer';
import styles from './EmailsPage.module.css';

export default function EmailsPage({ refreshKey }) {
  const [selectedEmail, setSelectedEmail] = useState(null);

  return (
    <div className={styles.wrapper}>
      <EmailTable
        refreshKey={refreshKey}
        onSelectEmail={(email) => setSelectedEmail(email)}
      />
      {selectedEmail && (
        <EmailDetailDrawer
          email={selectedEmail}
          onClose={() => setSelectedEmail(null)}
        />
      )}
    </div>
  );
}
